package dev.raflos.cocina.data

import dev.raflos.cocina.data.local.AppStateDao
import dev.raflos.cocina.data.local.AppStateEntity
import dev.raflos.cocina.data.model.AppState
import dev.raflos.cocina.data.model.DespensaFile
import dev.raflos.cocina.data.model.ListaFile
import dev.raflos.cocina.data.model.RecetasFile
import dev.raflos.cocina.data.model.toDespensaFile
import dev.raflos.cocina.data.model.toListaFile
import dev.raflos.cocina.data.model.toRecetasFile
import dev.raflos.cocina.data.model.withDespensaFile
import dev.raflos.cocina.data.model.withListaFile
import dev.raflos.cocina.data.model.withRecetasFile
import dev.raflos.cocina.data.remote.GithubConflictException
import dev.raflos.cocina.data.remote.GithubDataSource
import dev.raflos.cocina.data.remote.GithubNotFoundException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

private const val DESPENSA_PATH = "data/despensa.json"
private const val RECETAS_PATH = "data/recetas.json"
private const val LISTA_PATH = "data/lista-de-compra.json"

sealed class SyncResult {
    data object Success : SyncResult()
    data class Failure(val error: Throwable) : SyncResult()
}

/**
 * Fuente de verdad local (Room) + reconciliación con el repo de GitHub, que hace de backend
 * (ver data/README.md). Offline-first: toda edición se guarda de inmediato en Room y se refleja
 * en la UI; el push/pull contra GitHub pasa en segundo plano (ver [sync]).
 *
 * El estado en memoria sigue siendo un [AppState] único, pero se persiste como tres archivos
 * independientes: cada uno lleva su `sha` (concurrencia optimista de la Contents API) y su flag
 * `dirty`, y se commitea por separado — last-write-wins a nivel de archivo.
 */
class SyncRepository(
    private val dao: AppStateDao,
    private val ds: GithubDataSource,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutex = Mutex()

    private val _state = MutableStateFlow<AppState?>(null)
    val state: StateFlow<AppState?> = _state.asStateFlow()

    private var despensaSha: String? = null
    private var recetasSha: String? = null
    private var listaSha: String? = null

    private var despensaDirty = false
    private var recetasDirty = false
    private var listaDirty = false

    private val pendingSync: Boolean get() = despensaDirty || recetasDirty || listaDirty

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var debouncedPush: Job? = null

    suspend fun initialize() = mutex.withLock {
        val local = dao.get()
        if (local != null) {
            _state.value = decode(local.json)
            despensaSha = local.despensaSha
            recetasSha = local.recetasSha
            listaSha = local.listaSha
            despensaDirty = local.despensaDirty
            recetasDirty = local.recetasDirty
            listaDirty = local.listaDirty
        }

        try {
            // Si hay ediciones locales sin commitear, primero las subimos (no las pisamos con
            // lo que haya en el repo); si no, traemos los tres archivos.
            if (pendingSync) pushLocked() else pullLocked()
        } catch (e: Exception) {
            if (local == null) {
                // Sin cache y sin poder leer el repo: sembramos los datos de ejemplo para que la
                // app sea usable. Solo los marcamos para commitear si los archivos realmente no
                // existen (404); si fue un problema de red, el próximo sync() hace pull en vez
                // de pisar los datos buenos del repo con la semilla.
                val missing = e is GithubNotFoundException
                val seeded = seedState()
                _state.value = seeded
                despensaDirty = missing
                recetasDirty = missing
                listaDirty = missing
                persist(seeded)
                if (missing) {
                    try {
                        pushLocked()
                    } catch (e2: Exception) {
                        // sin red: queda pendiente para el próximo sync()
                    }
                }
            }
        }
    }

    suspend fun mutate(updater: (AppState) -> AppState) {
        mutex.withLock {
            val current = _state.value ?: return@withLock
            val next = updater(current)
            _state.value = next
            // Solo se ensucia el archivo cuya rebanada del estado cambió realmente; una mutación
            // puede tocar dos (ej. agregar un artículo toca despensa y lista) y son dos commits.
            // Se acumulan con OR: lo que quedó sucio antes sigue sucio.
            despensaDirty = despensaDirty || current.toDespensaFile() != next.toDespensaFile()
            recetasDirty = recetasDirty || current.toRecetasFile() != next.toRecetasFile()
            listaDirty = listaDirty || current.toListaFile() != next.toListaFile()
            persist(next)
        }
        scheduleDebouncedPush()
    }

    /** Empuja los cambios a GitHub ~800ms después de la última edición, mientras haya red;
     * si falla (sin conexión), el cambio queda en Room con su flag dirty y se reintenta en
     * el próximo [sync] (resume de la app, reconexión). */
    private fun scheduleDebouncedPush() {
        debouncedPush?.cancel()
        debouncedPush = scope.launch {
            delay(800)
            mutex.withLock {
                try {
                    if (pendingSync) pushLocked()
                } catch (e: Exception) {
                    // sin red o GitHub no responde: se reintenta en el próximo sync()
                }
            }
        }
    }

    /** Push si hay cambios locales pendientes; si no, pull por si se editó desde otro lado. */
    suspend fun sync(): SyncResult = mutex.withLock {
        try {
            if (pendingSync) pushLocked() else pullLocked()
            SyncResult.Success
        } catch (e: Exception) {
            SyncResult.Failure(e)
        }
    }

    /** GET de los tres archivos en paralelo. No se pisa un archivo con cambios locales sin
     * commitear: ese se resuelve al subirlo (409 → reintento con el sha fresco). */
    private suspend fun pullLocked() {
        val remote = coroutineScope {
            listOf(
                async { ds.fetchFile(DESPENSA_PATH) },
                async { ds.fetchFile(RECETAS_PATH) },
                async { ds.fetchFile(LISTA_PATH) },
            ).awaitAll()
        }

        var next = _state.value ?: AppState()
        if (!despensaDirty) {
            next = next.withDespensaFile(json.decodeFromString(DespensaFile.serializer(), remote[0].first))
            despensaSha = remote[0].second
        }
        if (!recetasDirty) {
            next = next.withRecetasFile(json.decodeFromString(RecetasFile.serializer(), remote[1].first))
            recetasSha = remote[1].second
        }
        if (!listaDirty) {
            next = next.withListaFile(json.decodeFromString(ListaFile.serializer(), remote[2].first))
            listaSha = remote[2].second
        }
        _state.value = next
        persist(next)
    }

    /** Un PUT (commit) por archivo sucio. Si uno falla por red se intentan igual los otros y
     * solo ese queda pendiente. */
    private suspend fun pushLocked() {
        val current = _state.value ?: return
        var failure: Exception? = null

        if (despensaDirty) {
            try {
                despensaSha = pushFile(
                    DESPENSA_PATH,
                    json.encodeToString(DespensaFile.serializer(), current.toDespensaFile()),
                    despensaSha,
                    "Actualizar despensa desde la app",
                )
                despensaDirty = false
            } catch (e: Exception) {
                failure = e
            }
        }
        if (recetasDirty) {
            try {
                recetasSha = pushFile(
                    RECETAS_PATH,
                    json.encodeToString(RecetasFile.serializer(), current.toRecetasFile()),
                    recetasSha,
                    "Actualizar recetas desde la app",
                )
                recetasDirty = false
            } catch (e: Exception) {
                failure = e
            }
        }
        if (listaDirty) {
            try {
                listaSha = pushFile(
                    LISTA_PATH,
                    json.encodeToString(ListaFile.serializer(), current.toListaFile()),
                    listaSha,
                    "Actualizar lista de compra desde la app",
                )
                listaDirty = false
            } catch (e: Exception) {
                failure = e
            }
        }

        persist(current)
        failure?.let { throw it }
    }

    /** PUT con reintento en conflicto: releemos el archivo solo para tomar su `sha` fresco y
     * volvemos a escribir NUESTRA versión local (última escritura gana, sin merge). */
    private suspend fun pushFile(path: String, body: String, sha: String?, message: String): String =
        try {
            ds.putFile(path, body, sha, message)
        } catch (e: GithubConflictException) {
            val (_, freshSha) = ds.fetchFile(path)
            ds.putFile(path, body, freshSha, message)
        }

    private suspend fun persist(state: AppState) {
        dao.upsert(
            AppStateEntity(
                json = encode(state),
                despensaSha = despensaSha,
                recetasSha = recetasSha,
                listaSha = listaSha,
                despensaDirty = despensaDirty,
                recetasDirty = recetasDirty,
                listaDirty = listaDirty,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    private fun encode(state: AppState) = json.encodeToString(AppState.serializer(), state)
    private fun decode(text: String) = json.decodeFromString(AppState.serializer(), text)
}
