package dev.raflos.cocina.data.model

import kotlinx.serialization.Serializable

/**
 * Forma de los tres archivos de `data/` en el repo (ver data/README.md). El [AppState] en
 * memoria sigue siendo un documento único — la partición en archivos vive solo acá y en
 * SyncRepository, para no tocar el ViewModel ni las pantallas.
 */

/** `data/despensa.json` */
@Serializable
data class DespensaFile(
    val pages: List<PantryPage> = emptyList(),
    val activePageId: String? = null,
)

/** `data/recetas.json` */
@Serializable
data class RecetasFile(
    val cocina: List<Recipe> = emptyList(),
    val repo: List<Recipe> = emptyList(),
    val stepDone: Map<String, Boolean> = emptyMap(),
)

/** `data/lista-de-compra.json` — listas por comercio, historial de precios, marcas boicoteadas y
 * tickets. Mapeo 1:1 con el estado en memoria (mismo esquema que la web). */
@Serializable
data class ListaFile(
    val stores: List<Store> = emptyList(),
    val lists: List<ShoppingList> = emptyList(),
    val priceHistory: Map<String, Map<String, Double>> = emptyMap(),
    val boycottedBrands: Map<String, List<String>> = emptyMap(),
    val receipts: List<Receipt> = emptyList(),
)

// ---- AppState -> archivo ----

fun AppState.toDespensaFile() = DespensaFile(pages = pPages, activePageId = pActiveId)

fun AppState.toRecetasFile() = RecetasFile(cocina = recipes.cocina, repo = recipes.repo, stepDone = rDone)

fun AppState.toListaFile() = ListaFile(
    stores = stores,
    lists = lists,
    priceHistory = priceHistory,
    boycottedBrands = boycottedBrands,
    receipts = receipts,
)

// ---- archivo -> AppState ----

fun AppState.withDespensaFile(file: DespensaFile): AppState =
    // Sin página activa se muestra el índice de repisas (navegación en dos niveles).
    copy(pPages = file.pages, pActiveId = file.activePageId)

fun AppState.withRecetasFile(file: RecetasFile): AppState =
    copy(recipes = RecipeBook(cocina = file.cocina, repo = file.repo), rDone = file.stepDone)

fun AppState.withListaFile(file: ListaFile): AppState = copy(
    stores = file.stores,
    lists = file.lists,
    priceHistory = file.priceHistory,
    boycottedBrands = file.boycottedBrands,
    receipts = file.receipts,
)
