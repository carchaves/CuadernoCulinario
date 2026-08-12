package dev.raflos.cocina.data

import dev.raflos.cocina.data.local.AppStateDao
import dev.raflos.cocina.data.local.AppStateEntity
import dev.raflos.cocina.data.model.AppState
import dev.raflos.cocina.data.remote.CocinaApi
import dev.raflos.cocina.data.remote.NetworkModule
import dev.raflos.cocina.data.remote.StatePutRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

sealed class SyncResult {
    data object Success : SyncResult()
    data class Failure(val error: Throwable) : SyncResult()
}

/**
 * Fuente de verdad local (Room) + reconciliación con el servidor. Offline-first: toda edición se
 * guarda de inmediato en Room y se refleja en la UI; el push/pull al servidor pasa en segundo
 * plano (ver [sync]), con el mismo criterio de `revision`/409 (last-write-wins a nivel de
 * documento) que usa la página web (app/src/storage/serverStorage.ts).
 */
class SyncRepository(
    private val dao: AppStateDao,
    private val api: CocinaApi,
    private val network: NetworkModule,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val mutex = Mutex()

    private val _state = MutableStateFlow<AppState?>(null)
    val state: StateFlow<AppState?> = _state.asStateFlow()

    private var revision = 0
    private var pendingSync = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var debouncedPush: Job? = null

    suspend fun initialize() = mutex.withLock {
        val local = dao.get()
        if (local != null) {
            _state.value = decode(local.json)
            revision = local.revision
            pendingSync = local.pendingSync
        }

        try {
            if (pendingSync) {
                pushLocked()
            } else {
                val remote = api.getState()
                when {
                    remote.data != null && (local == null || remote.revision != revision) -> {
                        _state.value = remote.data
                        revision = remote.revision
                        pendingSync = false
                        persist(remote.data, revision, false)
                    }
                    remote.data == null && local == null -> {
                        val seeded = seedState()
                        _state.value = seeded
                        pendingSync = true
                        persist(seeded, 0, true)
                        pushLocked()
                    }
                }
            }
        } catch (e: Exception) {
            if (local == null) {
                val seeded = seedState()
                _state.value = seeded
                pendingSync = true
                persist(seeded, 0, true)
            }
        }
    }

    suspend fun mutate(updater: (AppState) -> AppState) {
        mutex.withLock {
            val current = _state.value ?: return@withLock
            val next = updater(current)
            _state.value = next
            pendingSync = true
            persist(next, revision, true)
        }
        scheduleDebouncedPush()
    }

    /** Empuja los cambios al servidor ~800ms después de la última edición, mientras haya red;
     * si falla (sin conexión), el cambio queda en Room con pendingSync=true y se reintenta en
     * el próximo [sync] (resume de la app, reconexión). */
    private fun scheduleDebouncedPush() {
        debouncedPush?.cancel()
        debouncedPush = scope.launch {
            delay(800)
            mutex.withLock {
                try {
                    if (pendingSync) pushLocked()
                } catch (e: Exception) {
                    // sin red o el servidor no responde: se reintenta en el próximo sync()
                }
            }
        }
    }

    /** Push si hay cambios locales pendientes; si no, pull por si otro dispositivo editó. */
    suspend fun sync(): SyncResult = mutex.withLock {
        try {
            if (pendingSync) pushLocked() else pullLocked()
            SyncResult.Success
        } catch (e: Exception) {
            SyncResult.Failure(e)
        }
    }

    private suspend fun pullLocked() {
        val remote = api.getState()
        if (remote.data != null && remote.revision != revision) {
            _state.value = remote.data
            revision = remote.revision
            pendingSync = false
            persist(remote.data, revision, false)
        }
    }

    private suspend fun pushLocked() {
        val current = _state.value ?: return
        var response = api.putState(StatePutRequest(current, revision))
        if (response.code() == 409) {
            val conflict = network.parseConflict(response.errorBody()?.string())
            // Última escritura gana: reintentamos nuestro estado sobre la revisión nueva del servidor.
            response = api.putState(StatePutRequest(current, conflict?.revision ?: revision))
        }
        if (response.isSuccessful) {
            val body = response.body()
            revision = body?.revision ?: revision
            pendingSync = false
            persist(current, revision, false)
        }
    }

    private suspend fun persist(state: AppState, revision: Int, pendingSync: Boolean) {
        dao.upsert(
            AppStateEntity(
                json = encode(state),
                revision = revision,
                pendingSync = pendingSync,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    private fun encode(state: AppState) = json.encodeToString(AppState.serializer(), state)
    private fun decode(text: String) = json.decodeFromString(AppState.serializer(), text)
}
