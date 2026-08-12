package dev.raflos.cocina.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.raflos.cocina.data.SyncRepository
import dev.raflos.cocina.data.roundFor
import dev.raflos.cocina.data.stepFor
import dev.raflos.cocina.data.stepId
import dev.raflos.cocina.data.uid
import dev.raflos.cocina.data.model.AppState
import dev.raflos.cocina.data.model.Ingredient
import dev.raflos.cocina.data.model.PantryPage
import dev.raflos.cocina.data.model.Recipe
import dev.raflos.cocina.data.model.RecipeStep
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RecipeDraft(
    val title: String,
    val meta: List<String>,
    val tiempo: String,
    val ingredientes: List<String>,
    val utensilios: List<String>,
    val prePrep: List<RecipeStep>,
    val prep: List<RecipeStep>,
)

/** Equivalente a app/src/core/store.ts: un solo estado + acciones, compartido por todas las pantallas. */
class AppViewModel(private val repo: SyncRepository) : ViewModel() {

    val state: StateFlow<AppState?> = repo.state

    init {
        viewModelScope.launch { repo.initialize() }
    }

    fun syncNow() {
        viewModelScope.launch { repo.sync() }
    }

    private fun mutate(updater: (AppState) -> AppState) {
        viewModelScope.launch { repo.mutate(updater) }
    }

    // ---- Despensa ----
    fun addPage(name: String): String {
        val id = uid()
        mutate { s -> s.copy(pPages = s.pPages + PantryPage(id, name), pActiveId = id) }
        return id
    }

    fun deletePage(pageId: String) = mutate { s ->
        val next = s.pPages.filter { it.id != pageId }
        s.copy(pPages = next, pActiveId = if (s.pActiveId == pageId) next.firstOrNull()?.id else s.pActiveId)
    }

    fun setActivePage(pageId: String) = mutate { it.copy(pActiveId = pageId) }

    fun addIngredient(pageId: String, name: String, type: String, amount: Double, unit: String) = mutate { s ->
        val ing = Ingredient(uid(), name, type, amount, unit)
        s.copy(pPages = s.pPages.map { if (it.id == pageId) it.copy(ingredients = it.ingredients + ing) else it })
    }

    fun adjustIngredient(pageId: String, ingId: String, dir: Int) = mutate { s ->
        s.copy(pPages = s.pPages.map { page ->
            if (page.id != pageId) page else page.copy(ingredients = page.ingredients.map { ing ->
                if (ing.id != ingId) ing else ing.copy(amount = roundFor(ing.amount + dir * stepFor(ing.unit), ing.unit))
            })
        })
    }

    fun removeIngredient(pageId: String, ingId: String) = mutate { s ->
        s.copy(pPages = s.pPages.map { if (it.id == pageId) it.copy(ingredients = it.ingredients.filterNot { i -> i.id == ingId }) else it })
    }

    fun addIngredientToPantry(name: String) = mutate { s ->
        val pantryFlat = s.pPages.flatMap { it.ingredients }
        if (pantryFlat.any { it.name.equals(name, ignoreCase = true) }) return@mutate s
        val ing = Ingredient(uid(), name, "unidad", 0.0, "u")
        val otros = s.pPages.firstOrNull { it.name == "Otros" }
        val pages = if (otros != null) {
            s.pPages.map { if (it.id == otros.id) it.copy(ingredients = it.ingredients + ing) else it }
        } else {
            s.pPages + PantryPage(uid(), "Otros", listOf(ing))
        }
        s.copy(pPages = pages)
    }

    // ---- Lista de compra ----
    fun addShoppingItem(name: String, amount: Double, unit: String, pageId: String) = mutate { s ->
        val pantryFlat = s.pPages.flatMap { it.ingredients }
        if (pantryFlat.any { it.name.equals(name, ignoreCase = true) }) return@mutate s
        val ing = Ingredient(uid(), name, if (unit == "u") "unidad" else "peso", amount, unit)
        s.copy(pPages = s.pPages.map { if (it.id == pageId) it.copy(ingredients = it.ingredients + ing) else it })
    }

    fun toggleIncluded(ingId: String) = mutate { s -> s.copy(lIncluded = toggleFlag(s.lIncluded, ingId)) }
    fun toggleDone(ingId: String) = mutate { s -> s.copy(lDone = toggleFlag(s.lDone, ingId)) }
    fun removeFromLista(ingId: String) = mutate { s -> s.copy(lIncluded = s.lIncluded - ingId) }
    fun resetLista() = mutate { it.copy(lDone = emptyMap()) }

    private fun toggleFlag(map: Map<String, Boolean>, key: String): Map<String, Boolean> =
        if (map.containsKey(key)) map - key else map + (key to true)

    // ---- Recetas ----
    fun saveRecipe(view: String, editingId: String?, draft: RecipeDraft): String {
        val id = editingId ?: uid()
        mutate { s ->
            val list = s.recipes.forView(view)
            val updated = if (editingId != null) {
                list.map {
                    if (it.id == editingId) it.copy(
                        title = draft.title, meta = draft.meta, tiempo = draft.tiempo,
                        ingredientes = draft.ingredientes, utensilios = draft.utensilios,
                        prePrep = draft.prePrep, prep = draft.prep,
                    ) else it
                }
            } else {
                list + Recipe(id, draft.title, draft.meta, draft.tiempo, draft.ingredientes, draft.utensilios, draft.prePrep, draft.prep)
            }
            s.copy(recipes = s.recipes.withView(view, updated))
        }
        return id
    }

    fun deleteRecipe(view: String, id: String) = mutate { s ->
        s.copy(recipes = s.recipes.withView(view, s.recipes.forView(view).filterNot { it.id == id }))
    }

    fun toggleStep(id: String) = mutate { s -> s.copy(rDone = toggleFlag(s.rDone, id)) }

    fun saveStepText(view: String, recId: String, section: String, index: Int, text: String) = mutate { s ->
        val list = s.recipes.forView(view).map { r ->
            if (r.id != recId) r else if (section == "pre") {
                r.copy(prePrep = r.prePrep.mapIndexed { i, st -> if (i == index) st.copy(t = text) else st })
            } else {
                r.copy(prep = r.prep.mapIndexed { i, st -> if (i == index) st.copy(t = text) else st })
            }
        }
        s.copy(recipes = s.recipes.withView(view, list))
    }

    fun resetRecetas(view: String) = mutate { s ->
        val ids = mutableSetOf<String>()
        s.recipes.forView(view).forEach { r ->
            r.prePrep.indices.forEach { i -> ids += stepId(view, r.id, "pre", i) }
            r.prep.indices.forEach { i -> ids += stepId(view, r.id, "prep", i) }
        }
        s.copy(rDone = s.rDone - ids)
    }

    class Factory(private val repo: SyncRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(repo) as T
    }
}
