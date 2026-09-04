package dev.raflos.cocina.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Ingredient(
    val id: String,
    val name: String,
    val type: String, // "peso" | "unidad"
    val amount: Double,
    val unit: String, // "g" | "kg" | "ml" | "L" | "u" | ...
)

@Serializable
data class PantryPage(
    val id: String,
    val name: String,
    val ingredients: List<Ingredient> = emptyList(),
    /** Clave del set de íconos del diseño (ver `PantryIconOptions`); null = ícono por índice. */
    val iconId: String? = null,
)

@Serializable
data class RecipeStep(
    val t: String,
    val title: String? = null,
    val time: String? = null,
    /** Nota libre del paso, independiente del texto `t` (espejo de `RecipeStep.note` en la web). */
    val note: String? = null,
)

@Serializable
data class Recipe(
    val id: String,
    val title: String,
    val meta: List<String> = emptyList(),
    val tiempo: String,
    val ingredientes: List<String> = emptyList(),
    val utensilios: List<String> = emptyList(),
    val prePrep: List<RecipeStep> = emptyList(),
    val prep: List<RecipeStep> = emptyList(),
)

@Serializable
data class RecipeBook(
    val cocina: List<Recipe> = emptyList(),
    val repo: List<Recipe> = emptyList(),
) {
    fun forView(view: String): List<Recipe> = if (view == "repo") repo else cocina

    fun withView(view: String, recipes: List<Recipe>): RecipeBook =
        if (view == "repo") copy(repo = recipes) else copy(cocina = recipes)
}

// ---- Lista de compra (mismo esquema que app/src/core/types.ts y data/lista-de-compra.json) ----

/** Un comercio: los nombres de campo son los del JSON, no cambiarlos (los escribe también la web). */
@Serializable
data class Store(
    val id: String,
    val name: String,
    val color: String,
    val address: String? = null,
)

@Serializable
data class ShoppingListItem(
    val ingredientId: String,
    val quantity: Double? = null,
    val unit: String? = null,
    val bought: Boolean = false,
    val price: Double? = null,
)

/** Lista de un comercio; `finalizedAt == null` ⇒ sigue abierta. Fechas ISO-8601 UTC. */
@Serializable
data class ShoppingList(
    val id: String,
    val storeId: String,
    val createdAt: String,
    val finalizedAt: String? = null,
    val items: List<ShoppingListItem> = emptyList(),
)

/** Foto de ticket archivada en el repo (`data/receipts/<id>.jpg`). */
@Serializable
data class Receipt(
    val id: String,
    val listId: String,
    val photoPath: String,
    val createdAt: String,
)

@Serializable
data class AppState(
    val pPages: List<PantryPage> = emptyList(),
    val pActiveId: String? = null,
    val stores: List<Store> = emptyList(),
    val lists: List<ShoppingList> = emptyList(),
    /** priceHistory[ingredientId][storeId] = precio */
    val priceHistory: Map<String, Map<String, Double>> = emptyMap(),
    /** boycottedBrands[ingredientId] = ["MarcaX", ...] */
    val boycottedBrands: Map<String, List<String>> = emptyMap(),
    val receipts: List<Receipt> = emptyList(),
    val recipes: RecipeBook = RecipeBook(),
    val rDone: Map<String, Boolean> = emptyMap(),
) {
    /** Derivado (no se serializa): ingredientes que ya están en alguna lista abierta. Lo usa
     * Despensa para pintar el botón "enviar a la lista". */
    val lIncluded: Map<String, Boolean>
        get() = lists.filter { it.finalizedAt == null }
            .flatMap { it.items }
            .associate { it.ingredientId to true }
}
