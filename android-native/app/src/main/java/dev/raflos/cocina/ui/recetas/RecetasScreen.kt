package dev.raflos.cocina.ui.recetas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.raflos.cocina.data.hasAllIngredients
import dev.raflos.cocina.data.model.AppState
import dev.raflos.cocina.data.model.Recipe
import dev.raflos.cocina.data.parseMinutes
import dev.raflos.cocina.ui.AppViewModel
import dev.raflos.cocina.ui.theme.RecetasColors
import dev.raflos.cocina.ui.theme.SerifFamily

@Composable
fun RecetasScreen(state: AppState, vm: AppViewModel, onBack: () -> Unit) {
    var view by remember { mutableStateOf("cocina") }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var servings by remember { mutableIntStateOf(4) }
    var pantryLinked by remember { mutableStateOf(true) }

    val recipes = state.recipes.forView(view)
    val selected = selectedId?.let { id -> recipes.firstOrNull { it.id == id } }
    val editing = editingId?.let { id -> recipes.firstOrNull { it.id == id } }
    val pantryFlat = state.pPages.flatMap { it.ingredients }

    Column(Modifier.fillMaxSize().background(RecetasColors.paper)) {
        Column(Modifier.padding(16.dp)) {
            TextButton(onClick = onBack, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text("← Menú", color = RecetasColors.inkSoft, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("PARA", color = RecetasColors.inkSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = { if (servings > 1) servings-- }) { Icon(Icons.Filled.Remove, contentDescription = null, tint = RecetasColors.inkSoft) }
                Text("$servings", color = RecetasColors.ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                IconButton(onClick = { if (servings < 24) servings++ }) { Icon(Icons.Filled.Add, contentDescription = null, tint = RecetasColors.inkSoft) }
                Text("personas", color = RecetasColors.inkSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Recetas", fontFamily = SerifFamily, fontSize = 28.sp, color = RecetasColors.ink)
                Surface(
                    color = if (pantryLinked) RecetasColors.accent else RecetasColors.card,
                    shape = androidx.compose.foundation.shape.CircleShape,
                ) {
                    IconButton(onClick = { pantryLinked = !pantryLinked }) {
                        Icon(Icons.Outlined.Inventory2, contentDescription = "Despensa", tint = if (pantryLinked) RecetasColors.card else RecetasColors.ink)
                    }
                }
            }

            if (selectedId == null && !adding) {
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("cocina" to "🍲 Cocina", "repo" to "🧁 Repostería").forEach { (v, label) ->
                        Box(
                            Modifier
                                .background(if (view == v) RecetasColors.card else Color.Transparent, RoundedCornerShape(9.dp))
                                .clickable { view = v; selectedId = null }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) { Text(label, color = if (view == v) RecetasColors.ink else RecetasColors.inkSoft, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                    }
                }
            }
            if (selected != null) {
                Text(selected.title, color = RecetasColors.accent, fontFamily = SerifFamily, fontSize = 22.sp, modifier = Modifier.padding(top = 6.dp))
            }
            if (adding) {
                Text(if (editingId != null) "Editar receta" else "Nueva receta", color = RecetasColors.accent, fontFamily = SerifFamily, fontSize = 22.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }

        when {
            adding -> RecipeFormSection(
                view = view,
                editingId = editingId,
                editingRecipe = editing,
                pantryFlat = pantryFlat,
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
    onOpen: (String) -> Unit,
    onCreate: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        itemsIndexed(recipes) { idx, r ->
            var done = 0
            val total = r.prePrep.size + r.prep.size
            r.prePrep.indices.forEach { i -> if (rDone.containsKey("$view.${r.id}.pre.$i")) done++ }
            r.prep.indices.forEach { i -> if (rDone.containsKey("$view.${r.id}.prep.$i")) done++ }
            val available = hasAllIngredients(r.ingredientes, pantryNames)
            Surface(
                color = RecetasColors.card,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, RecetasColors.line),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onOpen(r.id) },
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Text("${idx + 1}", color = RecetasColors.accent, fontFamily = SerifFamily, fontSize = 20.sp, fontStyle = FontStyle.Italic, modifier = Modifier.width(28.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.title, fontFamily = SerifFamily, fontSize = 18.sp, color = RecetasColors.ink)
                        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            r.meta.forEach { Chip(it) }
                            Chip("⏱ ${r.tiempo}", accent = true)
                            if (available) Chip("🍅 disponible", accent = false, color = RecetasColors.en)
                        }
                    }
                    Text("$done/$total", color = RecetasColors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clickable(onClick = onCreate)
                    .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) { Text("+ Crear nueva receta", color = RecetasColors.accent, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun Chip(text: String, accent: Boolean = false, color: androidx.compose.ui.graphics.Color? = null) {
    Surface(color = color ?: if (accent) RecetasColors.accent.copy(alpha = 0.15f) else RecetasColors.line.copy(alpha = 0.4f), shape = RoundedCornerShape(999.dp)) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (color != null) androidx.compose.ui.graphics.Color.White else if (accent) RecetasColors.accent else RecetasColors.inkSoft, modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp))
    }
}
