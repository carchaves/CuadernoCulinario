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
)

@Serializable
data class RecipeStep(
    val t: String,
    val title: String? = null,
    val time: String? = null,
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

@Serializable
data class AppState(
    val pPages: List<PantryPage> = emptyList(),
    val pActiveId: String? = null,
    val lDone: Map<String, Boolean> = emptyMap(),
    val lIncluded: Map<String, Boolean> = emptyMap(),
    val recipes: RecipeBook = RecipeBook(),
    val rDone: Map<String, Boolean> = emptyMap(),
)
