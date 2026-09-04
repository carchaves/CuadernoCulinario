package dev.raflos.cocina.ui.recetas

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.raflos.cocina.data.hasAllIngredients
import dev.raflos.cocina.data.model.AppState
import dev.raflos.cocina.data.model.Recipe
import dev.raflos.cocina.data.parseMinutes
import dev.raflos.cocina.data.splitIngredient
import dev.raflos.cocina.ui.AppViewModel
import dev.raflos.cocina.ui.SystemBarsAppearance
import dev.raflos.cocina.ui.theme.DMSansFamily
import dev.raflos.cocina.ui.theme.InstrumentSerifFamily
import dev.raflos.cocina.ui.theme.PantryShelfIcon
import dev.raflos.cocina.ui.theme.PantryShelfOffIcon
import dev.raflos.cocina.ui.theme.RecetasColors

// Tonos que el diseño usa solo en Recetas y que no están en RecetasColors.
internal val ChipBg = Color(0xFFEFEADF)
internal val ChipAccentBg = Color(0xFFF3EAD9)
internal val MutedLabel = Color(0xFFA99A82)
private val DimStripe = Color(0xFFC9BFAC)
private val TabsBg = Color(0xFFEBE5D9)

@Composable
fun RecetasScreen(state: AppState, vm: AppViewModel, onBack: () -> Unit) {
    var view by remember { mutableStateOf("cocina") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var servings by remember { mutableIntStateOf(4) }
    var pantryLinked by remember { mutableStateOf(true) }
    var sortByTime by remember { mutableStateOf(false) }
    var filterByPantry by remember { mutableStateOf(false) }

    val recipes = state.recipes.forView(view)
    val selected = selectedId?.let { id -> recipes.firstOrNull { it.id == id } }
    val editing = editingId?.let { id -> recipes.firstOrNull { it.id == id } }
    val pantryFlat = state.pPages.flatMap { it.ingredients }

    SystemBarsAppearance(lightBackground = true)
    // Un paso por vez: si hay una receta abierta o el formulario, "atrás" vuelve a la lista;
    // si ya estamos en la lista, "atrás" vuelve al Menú (BackHandler de MainActivity).
    BackHandler(enabled = adding || selectedId != null) {
        adding = false
        editingId = null
        selectedId = null
    }
    Column(Modifier.fillMaxSize().background(RecetasColors.paper).windowInsetsPadding(WindowInsets.systemBars)) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "← MENÚ",
                color = RecetasColors.inkSoft,
                fontFamily = DMSansFamily,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
                modifier = Modifier.clickable(onClick = onBack).padding(bottom = 10.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("PARA", color = RecetasColors.inkSoft, fontFamily = DMSansFamily, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.6.sp)
                IconButton(onClick = { if (servings > 1) servings-- }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Remove, contentDescription = "Menos", tint = RecetasColors.inkSoft, modifier = Modifier.size(14.dp))
                }
                Text("$servings", color = RecetasColors.ink, fontFamily = DMSansFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                IconButton(onClick = { if (servings < 24) servings++ }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = "Más", tint = RecetasColors.inkSoft, modifier = Modifier.size(14.dp))
                }
                Text("PERSONAS", color = RecetasColors.inkSoft, fontFamily = DMSansFamily, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.6.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Recetas", fontFamily = InstrumentSerifFamily, fontSize = 30.sp, color = RecetasColors.ink)
                Box(
                    Modifier
                        .size(40.dp)
                        .background(if (pantryLinked) RecetasColors.accent else RecetasColors.card, CircleShape)
                        .border(1.dp, if (pantryLinked) RecetasColors.accent else RecetasColors.line, CircleShape)
                        .clickable { pantryLinked = !pantryLinked },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (pantryLinked) PantryShelfIcon else PantryShelfOffIcon,
                        contentDescription = "Usar Despensa",
                        tint = if (pantryLinked) RecetasColors.card else RecetasColors.ink,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            if (selectedId == null && !adding) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .background(TabsBg, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    listOf("cocina" to "🍲 Cocina", "repo" to "🧁 Repostería").forEach { (v, label) ->
                        Box(
                            Modifier
                                .weight(1f)
                                .background(if (view == v) RecetasColors.card else Color.Transparent, RoundedCornerShape(9.dp))
                                .clickable { view = v; selectedId = null }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(label, color = if (view == v) RecetasColors.ink else RecetasColors.inkSoft, fontFamily = DMSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
            }
            if (selected != null) {
                Text(selected.title, color = RecetasColors.accent, fontFamily = InstrumentSerifFamily, fontSize = 24.sp, modifier = Modifier.padding(top = 6.dp))
            }
            if (adding) {
                Text(if (editingId != null) "Editar receta" else "Nueva receta", color = RecetasColors.accent, fontFamily = InstrumentSerifFamily, fontSize = 24.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }

        when {
            adding -> RecipeFormSection(
                view = view,
                editingId = editingId,
                editingRecipe = editing,
                pantryFlat = pantryFlat,
                allRecipes = state.recipes.cocina + state.recipes.repo,
                vm = vm,
                onCancel = { adding = false },
                onSaved = { id -> adding = false; editingId = null; selectedId = id },
            )
            selected != null -> RecipeDetailSection(
                view = view,
                recipe = selected,
                servings = servings,
                pantryLinked = pantryLinked,
                pantryFlat = pantryFlat,
                rDone = state.rDone,
                vm = vm,
                onClose = { selectedId = null },
                onEdit = { editingId = selected.id; adding = true; selectedId = null },
            )
            else -> RecipeListSection(
                recipes = recipes,
                rDone = state.rDone,
                view = view,
                pantryNames = pantryFlat.map { it.name.lowercase() },
                sortByTime = sortByTime,
                onToggleSort = { sortByTime = !sortByTime },
                pantryLinked = pantryLinked,
                filterByPantry = filterByPantry,
                onToggleFilter = { filterByPantry = !filterByPantry },
                onOpen = { selectedId = it },
                onCreate = { editingId = null; adding = true },
            )
        }
    }
}

@Composable
private fun RecipeListSection(
    recipes: List<Recipe>,
    rDone: Map<String, Boolean>,
    view: String,
    pantryNames: List<String>,
    sortByTime: Boolean,
    onToggleSort: () -> Unit,
    pantryLinked: Boolean,
    filterByPantry: Boolean,
    onToggleFilter: () -> Unit,
    onOpen: (String) -> Unit,
    onCreate: () -> Unit,
) {
    // El número de tarjeta es el del orden original, no el del orden mostrado (como el diseño).
    var items = recipes.mapIndexed { idx, r -> RecipeCardData(r, idx + 1, rDone, view, pantryNames) }
    if (sortByTime) items = items.sortedBy { it.minutes }
    val available = if (filterByPantry) items.filter { it.available } else items
    val rest = if (filterByPantry) items.filterNot { it.available } else emptyList()
    var expandedId by remember { mutableStateOf<String?>(null) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                FilterPill("⏱ Menor tiempo", sortByTime, onToggleSort)
                if (pantryLinked) FilterPill("🍅 Ingredientes disponibles", filterByPantry, onToggleFilter)
            }
        }
        itemsIndexed(available, key = { _, it -> it.recipe.id }) { _, data ->
            RecipeCard(data, expandedId == data.recipe.id, { expandedId = if (expandedId == data.recipe.id) null else data.recipe.id }, { onOpen(data.recipe.id) })
        }
        if (available.isNotEmpty() && rest.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).height(1.dp).background(RecetasColors.line))
                    Text(
                        "FALTAN INGREDIENTES",
                        color = MutedLabel,
                        fontFamily = DMSansFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 10.dp),
                    )
                    Box(Modifier.weight(1f).height(1.dp).background(RecetasColors.line))
                }
            }
        }
        itemsIndexed(rest, key = { _, it -> it.recipe.id }) { _, data ->
            Box(Modifier.alpha(0.62f)) {
                RecipeCard(data, expandedId == data.recipe.id, { expandedId = if (expandedId == data.recipe.id) null else data.recipe.id }, { onOpen(data.recipe.id) }, dim = true)
            }
        }
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .dashedRoundedBorder(Color(0xFFC9BC9F), 14.dp)
                    .clickable(onClick = onCreate)
                    .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("+ Crear nueva receta", color = RecetasColors.accent, fontFamily = DMSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

private class RecipeCardData(
    val recipe: Recipe,
    val number: Int,
    rDone: Map<String, Boolean>,
    view: String,
    pantryNames: List<String>,
) {
    val total = recipe.prePrep.size + recipe.prep.size
    val done = recipe.prePrep.indices.count { rDone.containsKey("$view.${recipe.id}.pre.$it") } +
        recipe.prep.indices.count { rDone.containsKey("$view.${recipe.id}.prep.$it") }
    val available = hasAllIngredients(recipe.ingredientes, pantryNames)
    val minutes = parseMinutes(recipe.tiempo)

    /** Igual que el diseño: la descripción del panel es el primer paso ("título: texto"). */
    val desc: String = run {
        val first = recipe.prePrep.firstOrNull() ?: recipe.prep.firstOrNull()
        val raw = when {
            first == null -> "Receta sin descripción."
            !first.title.isNullOrBlank() && first.t.isNotBlank() -> "${first.title}: ${first.t}"
            first.t.isNotBlank() -> first.t
            else -> first.title ?: "Receta sin descripción."
        }
        if (raw.length > 150) raw.take(150).trim() + "…" else raw
    }
    private val ingNames = recipe.ingredientes.map { splitIngredient(it).name }
    val ingPreview = ingNames.take(9)
    val ingMore = if (ingNames.size > 9) "+${ingNames.size - 9} más" else null
    val ingCountLabel = "${ingNames.size} INGREDIENTES"
}

@Composable
private fun RecipeCard(
    data: RecipeCardData,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onOpen: () -> Unit,
    dim: Boolean = false,
) {
    val stripe = if (dim) DimStripe else RecetasColors.accent
    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Surface(
            color = RecetasColors.card,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, RecetasColors.line),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        ) {
            Row(Modifier.height(IntrinsicSize.Min)) {
                // Franja de acento a la izquierda.
                Box(Modifier.width(7.dp).fillMaxHeight().background(stripe.copy(alpha = 0.85f)))
                Column(Modifier.weight(1f).padding(start = 16.dp, end = 16.dp, top = 15.dp, bottom = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${data.number}",
                            color = RecetasColors.accent,
                            fontFamily = InstrumentSerifFamily,
                            fontStyle = FontStyle.Italic,
                            fontSize = 20.sp,
                        )
                        Spacer(Modifier.width(11.dp))
                        Text(
                            data.recipe.title,
                            fontFamily = InstrumentSerifFamily,
                            fontSize = 20.sp,
                            lineHeight = 22.sp,
                            color = RecetasColors.ink,
                            modifier = Modifier.weight(1f),
                        )
                        Text("${data.done}/${data.total}", color = RecetasColors.accent, fontFamily = DMSansFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Box(
                            Modifier.size(26.dp).clickable(onClick = onToggleExpand),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = if (expanded) "Ocultar vista previa" else "Ver vista previa",
                                tint = MutedLabel,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    DashedLine(Modifier.padding(top = 11.dp, bottom = 10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        data.recipe.meta.take(2).forEach { MetaChip(it) }
                        MetaChip("⏱ ${data.recipe.tiempo}", accent = true)
                    }
                    AnimatedVisibility(
                        visible = expanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Column {
                            DashedLine(Modifier.padding(top = 12.dp, bottom = 11.dp))
                            Text(data.desc, color = RecetasColors.inkSoft, fontFamily = DMSansFamily, fontSize = 13.sp, lineHeight = 19.sp)
                            Text(
                                data.ingCountLabel,
                                color = MutedLabel,
                                fontFamily = DMSansFamily,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.9.sp,
                                modifier = Modifier.padding(top = 12.dp, bottom = 7.dp),
                            )
                            // Vista previa de ingredientes en 2 columnas.
                            data.ingPreview.chunked(2).forEach { pair ->
                                Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    pair.forEach { ig ->
                                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                            Box(Modifier.size(4.dp).background(RecetasColors.accent, CircleShape))
                                            Spacer(Modifier.width(7.dp))
                                            Text(ig, color = RecetasColors.ink, fontFamily = DMSansFamily, fontSize = 13.sp)
                                        }
                                    }
                                    repeat(2 - pair.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                            data.ingMore?.let {
                                Text(it, color = MutedLabel, fontFamily = DMSansFamily, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
            }
        }
        // Bordes de "papel apilado" asomando debajo, como la sombra en capas del diseño.
        Box(
            Modifier
                .padding(horizontal = 5.dp)
                .fillMaxWidth()
                .height(4.dp)
                .background(RecetasColors.card, RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                .border(1.dp, RecetasColors.line, RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)),
        )
        Box(
            Modifier
                .padding(horizontal = 11.dp)
                .fillMaxWidth()
                .height(4.dp)
                .background(RecetasColors.card, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                .border(1.dp, RecetasColors.line, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)),
        )
    }
}

@Composable
private fun FilterPill(text: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .background(if (on) RecetasColors.accent else RecetasColors.card, RoundedCornerShape(9.dp))
            .border(1.dp, if (on) RecetasColors.accent else RecetasColors.line, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
    ) {
        Text(text, color = if (on) Color.White else RecetasColors.inkSoft, fontFamily = DMSansFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun MetaChip(text: String, accent: Boolean = false) {
    Surface(color = if (accent) ChipAccentBg else ChipBg, shape = RoundedCornerShape(20.dp)) {
        Text(
            text,
            fontFamily = DMSansFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (accent) RecetasColors.accent else RecetasColors.inkSoft,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
        )
    }
}

@Composable
internal fun DashedLine(modifier: Modifier = Modifier, color: Color = RecetasColors.line) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .drawBehind {
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2),
                    strokeWidth = 2f,
                    cap = StrokeCap.Butt,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f)),
                )
            },
    )
}

internal fun Modifier.dashedRoundedBorder(color: Color, radius: androidx.compose.ui.unit.Dp) = this.drawBehind {
    val r = radius.toPx()
    drawRoundRect(
        color = color,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 6f)),
        ),
    )
}
