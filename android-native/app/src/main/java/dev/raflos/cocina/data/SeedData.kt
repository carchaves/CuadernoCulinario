package dev.raflos.cocina.data

import dev.raflos.cocina.data.model.AppState
import dev.raflos.cocina.data.model.Ingredient
import dev.raflos.cocina.data.model.PantryPage
import dev.raflos.cocina.data.model.Recipe
import dev.raflos.cocina.data.model.RecipeBook
import dev.raflos.cocina.data.model.RecipeStep

/** Port of app/src/core/seed.ts — misma despensa/recetas de ejemplo, para que el primer
 * dispositivo que se loguea (celular o web) siembre los mismos datos iniciales. */

fun seedPantry(): List<PantryPage> = listOf(
    PantryPage(
        id = uid(), name = "Granos y secos",
        ingredients = listOf(
            Ingredient(uid(), "Arroz", "peso", 2.0, "kg"),
            Ingredient(uid(), "Lentejas", "peso", 500.0, "g"),
            Ingredient(uid(), "Harina", "peso", 1.0, "kg"),
        ),
    ),
    PantryPage(
        id = uid(), name = "Frescos",
        ingredients = listOf(
            Ingredient(uid(), "Huevos", "unidad", 12.0, "u"),
            Ingredient(uid(), "Leche", "peso", 1.0, "L"),
        ),
    ),
    PantryPage(
        id = uid(), name = "Especias",
        ingredients = listOf(
            Ingredient(uid(), "Sal", "peso", 500.0, "g"),
            Ingredient(uid(), "Comino", "unidad", 2.0, "u"),
        ),
    ),
    PantryPage(
        id = uid(), name = "Carne deshebrada",
        ingredients = listOf(
            Ingredient(uid(), "Posta o pecho de res", "peso", 1.3, "kg"),
            Ingredient(uid(), "Cebolla", "unidad", 2.0, "u"),
            Ingredient(uid(), "Pimiento", "unidad", 1.0, "u"),
            Ingredient(uid(), "Ajo", "unidad", 4.0, "u"),
            Ingredient(uid(), "Tomate", "unidad", 2.0, "u"),
            Ingredient(uid(), "Pasta de tomate", "peso", 100.0, "g"),
            Ingredient(uid(), "Comino", "peso", 20.0, "g"),
            Ingredient(uid(), "Orégano", "peso", 20.0, "g"),
            Ingredient(uid(), "Laurel", "unidad", 3.0, "u"),
            Ingredient(uid(), "Sal", "peso", 200.0, "g"),
            Ingredient(uid(), "Pimienta", "peso", 50.0, "g"),
            Ingredient(uid(), "Caldo", "peso", 250.0, "ml"),
        ),
    ),
)

fun seedRecipes(): RecipeBook = RecipeBook(
    cocina = listOf(
        Recipe(
            id = "baleadas-armadas", title = "Baleadas con carne, frijol, huevo y aguacate",
            meta = listOf("Ensamblado", "Individual"), tiempo = "≈ 10 min",
            ingredientes = listOf(
                "4 tortillas de harina", "2 tazas de carne deshebrada", "1 taza de frijol refrito",
                "4 huevos", "1 aguacate", "Sal",
            ),
            utensilios = listOf("Sartén", "Comal", "Espátula"),
            prePrep = listOf(RecipeStep(t = "Calentar por separado la carne deshebrada y el frijol refrito.")),
            prep = listOf(
                RecipeStep(t = "Freír los huevos."),
                RecipeStep(t = "Calentar las tortillas en el comal."),
                RecipeStep(t = "Rellenar cada tortilla con frijol, carne, huevo frito y aguacate."),
            ),
        ),
        Recipe(
            id = "pepian-recalentado", title = "Pepián recalentado con arroz",
            meta = listOf("Recalentado", "Rápido"), tiempo = "≈ 10 min",
            ingredientes = listOf("Pepián (sobrante)", "Arroz cocido"),
            utensilios = listOf("Olla pequeña", "Cuchara de madera"),
            prePrep = listOf(RecipeStep(t = "Sacar el pepián y el arroz del refrigerador.")),
            prep = listOf(
                RecipeStep(t = "Calentar el pepián a fuego lento, agregando agua o caldo si está muy espeso."),
                RecipeStep(t = "Calentar el arroz."),
                RecipeStep(t = "Servir el pepián sobre el arroz."),
            ),
        ),
        Recipe(
            id = "pupusas-freezer", title = "Pupusas del freezer al comal + curtido",
            meta = listOf("Del freezer", "Rápido"), tiempo = "≈ 15 min",
            ingredientes = listOf("Pupusas congeladas", "Curtido"),
            utensilios = listOf("Comal", "Tenazas"),
            prePrep = listOf(RecipeStep(t = "Sacar las pupusas congeladas y el curtido del refrigerador.")),
            prep = listOf(
                RecipeStep(t = "Cocinar las pupusas directo del freezer en comal a fuego medio-bajo, sin descongelar.", time = "5–6 min por lado"),
                RecipeStep(t = "Servir calientes con curtido."),
            ),
        ),
        Recipe(
            id = "bowl-gallo-pinto", title = "Bowl de gallo pinto, carne, huevo y plátano",
            meta = listOf("Bowl", "Desayuno o cena"), tiempo = "≈ 15 min",
            ingredientes = listOf(
                "Gallo pinto (sobrante)", "1 taza de carne deshebrada", "2 huevos",
                "1 plátano maduro", "Aceite",
            ),
            utensilios = listOf("Sartén", "Espátula"),
            prePrep = listOf(RecipeStep(t = "Cortar el plátano maduro en rodajas.")),
            prep = listOf(
                RecipeStep(t = "Freír el plátano maduro hasta dorar."),
                RecipeStep(t = "Calentar el gallo pinto y la carne deshebrada."),
                RecipeStep(t = "Freír los huevos."),
                RecipeStep(t = "Armar el bowl con gallo pinto, carne, huevo y plátano."),
            ),
        ),
        Recipe(
            id = "tacos-carne-deshebrada", title = "Tacos de carne deshebrada, chimol, aguacate y queso",
            meta = listOf("Tacos o plato", "Ensamblado"), tiempo = "≈ 15 min",
            ingredientes = listOf(
                "Tortillas de maíz o harina", "2 tazas de carne deshebrada",
                "Chimol (tomate, cebolla, culantro, limón)", "1 aguacate", "Queso que derrita",
            ),
            utensilios = listOf("Comal", "Tabla y cuchillo"),
            prePrep = listOf(RecipeStep(t = "Preparar el chimol: picar tomate, cebolla y culantro; sazonar con limón y sal.")),
            prep = listOf(
                RecipeStep(t = "Calentar la carne deshebrada y las tortillas."),
                RecipeStep(t = "Armar tacos o plato con carne, chimol, aguacate y queso."),
            ),
        ),
    ),
    repo = listOf(
        Recipe(
            id = "barras-datil", title = "Barras de dátil, avena y maní",
            meta = listOf("Sin horno", "~12"), tiempo = "≈ 20 min + 1 h refrigeración",
            ingredientes = listOf(
                "Avena", "Nueces", "250 g de dátiles", "Mantequilla de maní", "Miel",
                "Leche en polvo", "Sal", "Chocolate para cubrir",
            ),
            utensilios = listOf("Procesador de alimentos", "Molde rectangular", "Refrigerador"),
            prePrep = listOf(
                RecipeStep(t = "Tostar avena y nueces.", time = "3–4 min"),
                RecipeStep(t = "Procesar los dátiles hasta formar una pasta."),
            ),
            prep = listOf(
                RecipeStep(t = "Sumar mantequilla de maní, miel, leche en polvo y sal a los dátiles."),
                RecipeStep(t = "Incorporar la avena y nueces en pulsos."),
                RecipeStep(t = "Presionar en el molde y cubrir con chocolate."),
                RecipeStep(t = "Refrigerar y cortar en frío.", time = "1 h"),
            ),
        ),
        Recipe(
            id = "bolitas", title = "Bolitas de avena y cacao",
            meta = listOf("Sin horno", "~20"), tiempo = "≈ 20 min",
            ingredientes = listOf("Avena", "Mantequilla de maní", "Leche en polvo", "Cacao amargo", "Miel", "Chía", "Coco rallado"),
            utensilios = listOf("Bowl", "Refrigerador"),
            prePrep = listOf(
                RecipeStep(t = "Mezclar avena, mantequilla de maní, leche en polvo, cacao, miel y chía."),
                RecipeStep(t = "Refrigerar la mezcla.", time = "15 min"),
            ),
            prep = listOf(RecipeStep(t = "Bolear la mezcla."), RecipeStep(t = "Rebozar en coco rallado.")),
        ),
        Recipe(
            id = "barras-platano", title = "Barras de avena con plátano",
            meta = listOf("Horno", "~12"), tiempo = "≈ 30 min",
            ingredientes = listOf(
                "Plátanos maduros", "Huevos", "Yogur griego", "Miel", "Avena", "Leche en polvo",
                "Polvo de hornear", "Canela", "Nueces", "Chispas de chocolate",
            ),
            utensilios = listOf("Horno", "Molde rectangular", "Bowl"),
            prePrep = listOf(RecipeStep(t = "Pisar los plátanos con huevos, yogur y miel.")),
            prep = listOf(
                RecipeStep(t = "Sumar avena, leche en polvo, polvo de hornear, canela, nueces y chispas."),
                RecipeStep(t = "Hornear a 180 °C.", time = "22–25 min · 180°"),
                RecipeStep(t = "Enfriar antes de cortar."),
            ),
        ),
        Recipe(
            id = "granola", title = "Granola en clusters",
            meta = listOf("Horno"), tiempo = "≈ 30 min",
            ingredientes = listOf(
                "1 clara de huevo", "Avena", "Frutos secos", "Semillas", "Leche en polvo", "Canela",
                "Miel", "Aceite de coco", "Coco rallado",
            ),
            utensilios = listOf("Horno", "Bandeja para hornear", "Bowl"),
            prePrep = listOf(
                RecipeStep(t = "Batir la clara de huevo."),
                RecipeStep(t = "Mezclar avena, frutos secos, semillas, leche en polvo y canela con la clara, miel y aceite de coco."),
            ),
            prep = listOf(
                RecipeStep(t = "Hornear sin revolver los primeros 20 min.", time = "25–30 min · 150°"),
                RecipeStep(t = "Sumar el coco al final."),
                RecipeStep(t = "Enfriar sin tocar para que queden clusters."),
            ),
        ),
    ),
)

fun seedState(): AppState {
    val pantry = seedPantry()
    return AppState(pPages = pantry, pActiveId = pantry.first().id, recipes = seedRecipes())
}
