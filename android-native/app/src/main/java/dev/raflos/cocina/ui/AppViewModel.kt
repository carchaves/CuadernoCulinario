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
import dev.raflos.cocina.data.model.Receipt
import dev.raflos.cocina.data.model.ShoppingList
import dev.raflos.cocina.data.model.ShoppingListItem
import dev.raflos.cocina.data.model.Store
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Comercio por defecto de la acción rápida "enviar a la lista" (mismo nombre que en los datos
 * migrados, para no duplicarlo). */
private const val DEFAULT_STORE_NAME = "Mi lista"
private const val DEFAULT_STORE_COLOR = "#8C8377"

/** ISO-8601 en UTC, igual que lo escribe la web. */
fun nowIso(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT)
    .apply { timeZone = TimeZone.getTimeZone("UTC") }
    .format(java.util.Date())

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

    fun setActivePage(pageId: String?) = mutate { it.copy(pActiveId = pageId) }

    fun setPageIcon(pageId: String, iconId: String) = mutate { s ->
        s.copy(pPages = s.pPages.map { if (it.id == pageId) it.copy(iconId = iconId) else it })
    }

    /** "Enviar a la lista" (Despensa): mete el ingrediente en la lista abierta más reciente; si no
     * hay ninguna abierta, crea una bajo el comercio genérico "Mi lista". */
    fun sendIngredientToLista(ingId: String) = mutate { s -> addIngredientToActiveList(s, ingId) }

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

    // -- comercios --

    fun createStore(name: String, color: String, address: String?): String {
        val id = uid()
        mutate { s ->
            s.copy(stores = s.stores + Store(id, name.trim(), color, address?.trim()?.ifEmpty { null }))
        }
        return id
    }

    fun deleteStore(storeId: String) = mutate { s ->
        s.copy(
            stores = s.stores.filterNot { it.id == storeId },
            lists = s.lists.filterNot { it.storeId == storeId },
        )
    }

    // -- listas --

    /** Crea una lista vacía para [storeId] y devuelve su id. */
    fun createList(storeId: String): String {
        val id = uid()
        mutate { s -> s.copy(lists = s.lists + ShoppingList(id, storeId, nowIso())) }
        return id
    }

    fun deleteList(listId: String) = mutate { s -> s.copy(lists = s.lists.filterNot { it.id == listId }) }

    fun toggleItemInList(listId: String, ingId: String) = mutate { s ->
        s.mapList(listId) { l ->
            if (l.items.any { it.ingredientId == ingId }) {
                l.copy(items = l.items.filterNot { it.ingredientId == ingId })
            } else {
                l.copy(items = l.items + ShoppingListItem(ingredientId = ingId))
            }
        }
    }

    fun removeItemFromList(listId: String, ingId: String) = mutate { s ->
        s.mapList(listId) { l -> l.copy(items = l.items.filterNot { it.ingredientId == ingId }) }
    }

    fun setItemQuantity(listId: String, ingId: String, quantity: Double?, unit: String?) = mutate { s ->
        s.mapList(listId) { l ->
            l.copy(items = l.items.map { if (it.ingredientId == ingId) it.copy(quantity = quantity, unit = unit) else it })
        }
    }

    fun toggleBought(listId: String, ingId: String) = mutate { s ->
        s.mapList(listId) { l ->
            l.copy(items = l.items.map { if (it.ingredientId == ingId) it.copy(bought = !it.bought) else it })
        }
    }

    /** "Comparar precios": manda el artículo a la lista abierta del comercio más barato,
     * creándola si ese comercio todavía no tiene una. */
    fun addIngredientToStoreList(ingId: String, storeId: String) = mutate { s ->
        val open = s.lists.lastOrNull { it.storeId == storeId && it.finalizedAt == null }
        if (open == null) {
            val nueva = ShoppingList(uid(), storeId, nowIso(), items = listOf(ShoppingListItem(ingredientId = ingId)))
            s.copy(lists = s.lists + nueva)
        } else if (open.items.any { it.ingredientId == ingId }) {
            s
        } else {
            s.mapList(open.id) { it.copy(items = it.items + ShoppingListItem(ingredientId = ingId)) }
        }
    }

    // -- marcas boicoteadas --

    fun addBoycottedBrand(ingId: String, brand: String) = mutate { s ->
        val clean = brand.trim()
        if (clean.isEmpty()) return@mutate s
        val actuales = s.boycottedBrands[ingId].orEmpty()
        if (actuales.any { it.equals(clean, ignoreCase = true) }) return@mutate s
        s.copy(boycottedBrands = s.boycottedBrands + (ingId to actuales + clean))
    }

    fun removeBoycottedBrand(ingId: String, brand: String) = mutate { s ->
        val restantes = s.boycottedBrands[ingId].orEmpty().filterNot { it == brand }
        s.copy(
            boycottedBrands = if (restantes.isEmpty()) s.boycottedBrands - ingId
            else s.boycottedBrands + (ingId to restantes),
        )
    }

    // -- precios / finalizar compra --

    fun setPrice(ingId: String, storeId: String, price: Double?) = mutate { s -> s.withPrice(ingId, storeId, price) }

    /** Sube la foto del ticket al repo y anota el [Receipt]. Si falla (sin red), no bloquea:
     * la compra se puede finalizar igual ("Seguir sin foto"). */
    fun uploadReceipt(listId: String, bytes: ByteArray, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            val id = uid()
            val path = "data/receipts/$id.jpg"
            val ok = try {
                repo.uploadReceiptPhoto(path, bytes)
                true
            } catch (e: Exception) {
                false
            }
            if (ok) {
                repo.mutate { s -> s.copy(receipts = s.receipts + Receipt(id, listId, path, nowIso())) }
            }
            onDone(ok)
        }
    }

    /** Cierra la compra: guarda los precios tipeados a mano en cada ítem y en el historial del
     * comercio, y marca la lista como finalizada. */
    fun finalizeList(listId: String, precios: Map<String, Double?>) = mutate { s ->
        val lista = s.lists.firstOrNull { it.id == listId } ?: return@mutate s
        var next = s.mapList(listId) { l ->
            l.copy(
                finalizedAt = nowIso(),
                items = l.items.map { it.copy(price = precios[it.ingredientId] ?: it.price) },
            )
        }
        precios.forEach { (ingId, price) ->
            if (price != null) next = next.withPrice(ingId, lista.storeId, price)
        }
        next
    }

    // -- helpers de lista --

    private fun AppState.mapList(listId: String, f: (ShoppingList) -> ShoppingList): AppState =
        copy(lists = lists.map { if (it.id == listId) f(it) else it })

    private fun AppState.withPrice(ingId: String, storeId: String, price: Double?): AppState {
        val porComercio = priceHistory[ingId].orEmpty()
        val next = if (price == null) porComercio - storeId else porComercio + (storeId to price)
        return copy(
            priceHistory = if (next.isEmpty()) priceHistory - ingId else priceHistory + (ingId to next),
        )
    }

    /** Agrega el ingrediente a la lista abierta más reciente, creando comercio/lista por defecto
     * la primera vez (el "Mi lista" que ya usan los datos migrados). */
    private fun addIngredientToActiveList(s: AppState, ingId: String): AppState {
        val abierta = s.lists.lastOrNull { it.finalizedAt == null }
        if (abierta != null) {
            if (abierta.items.any { it.ingredientId == ingId }) return s
            return s.mapList(abierta.id) { it.copy(items = it.items + ShoppingListItem(ingredientId = ingId)) }
        }
        val store = s.stores.firstOrNull { it.name == DEFAULT_STORE_NAME }
            ?: Store(uid(), DEFAULT_STORE_NAME, DEFAULT_STORE_COLOR, null)
        val stores = if (s.stores.any { it.id == store.id }) s.stores else s.stores + store
        val nueva = ShoppingList(uid(), store.id, nowIso(), items = listOf(ShoppingListItem(ingredientId = ingId)))
        return s.copy(stores = stores, lists = s.lists + nueva)
    }

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

    /** La nota es un campo aparte del texto del paso: se edita con su propia acción. */
    fun saveStepNote(view: String, recId: String, section: String, index: Int, note: String) = mutate { s ->
        val value = note.trim().ifEmpty { null }
        val list = s.recipes.forView(view).map { r ->
            if (r.id != recId) r else if (section == "pre") {
                r.copy(prePrep = r.prePrep.mapIndexed { i, st -> if (i == index) st.copy(note = value) else st })
            } else {
                r.copy(prep = r.prep.mapIndexed { i, st -> if (i == index) st.copy(note = value) else st })
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
