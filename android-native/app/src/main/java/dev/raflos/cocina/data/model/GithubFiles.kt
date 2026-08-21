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

/** `data/lista-de-compra.json` — como el archivo se reescribe entero, arrays planos de ids
 * (más legibles en un diff de git) en vez del `Record<id, true>` interno. */
@Serializable
data class ListaFile(
    val includedIngredientIds: List<String> = emptyList(),
    val doneIngredientIds: List<String> = emptyList(),
)

// ---- AppState -> archivo ----

fun AppState.toDespensaFile() = DespensaFile(pages = pPages, activePageId = pActiveId)

fun AppState.toRecetasFile() = RecetasFile(cocina = recipes.cocina, repo = recipes.repo, stepDone = rDone)

fun AppState.toListaFile() = ListaFile(
    includedIngredientIds = lIncluded.filterValues { it }.keys.toList(),
    doneIngredientIds = lDone.filterValues { it }.keys.toList(),
)

// ---- archivo -> AppState ----

fun AppState.withDespensaFile(file: DespensaFile): AppState =
    copy(pPages = file.pages, pActiveId = file.activePageId ?: file.pages.firstOrNull()?.id)

fun AppState.withRecetasFile(file: RecetasFile): AppState =
    copy(recipes = RecipeBook(cocina = file.cocina, repo = file.repo), rDone = file.stepDone)

fun AppState.withListaFile(file: ListaFile): AppState = copy(
    lIncluded = file.includedIngredientIds.associateWith { true },
    lDone = file.doneIngredientIds.associateWith { true },
)
