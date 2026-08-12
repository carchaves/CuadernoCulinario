package dev.raflos.cocina.ui.recetas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.raflos.cocina.data.computeIngredientAvailability
import dev.raflos.cocina.data.model.Ingredient
import dev.raflos.cocina.data.model.Recipe
import dev.raflos.cocina.data.stepId
import dev.raflos.cocina.ui.AppViewModel
import dev.raflos.cocina.ui.theme.RecetasColors
import dev.raflos.cocina.ui.theme.SerifFamily

@Composable
fun RecipeDetailSection(
    view: String,
    recipe: Recipe,
    servings: Int,
    pantryLinked: Boolean,
    pantryFlat: List<Ingredient>,
    rDone: Map<String, Boolean>,
    vm: AppViewModel,
    onClose: () -> Unit,
    onEdit: () -> Unit,
) {
    val availability = remember(recipe, pantryFlat, servings) { computeIngredientAvailability(recipe.ingredientes, pantryFlat, servings) }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            TextButton(onClick = onClose, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text("← Recetas", color = RecetasColors.inkSoft, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        item {
            Card(title = "Ingredientes") {
                if (!pantryLinked) {
                    recipe.ingredientes.forEach { IngredientRow(it, RecetasColors.accent) }
                } else {
                    if (availability.ingEn.isNotEmpty()) {
                        Text("EN DESPENSA", color = RecetasColors.en, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                        availability.ingEn.forEach { AvailabilityRow(it.name, it.numDisplay, it.unitDisplay, RecetasColors.en) }
                    }
                    if (availability.ingPocas.isNotEmpty()) {
                        Text("POCAS UNIDADES", color = RecetasColors.pocas, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                        availability.ingPocas.forEach { AvailabilityRow(it.name, it.numDisplay, it.unitDisplay, RecetasColors.pocas) }
                    }
                    if (availability.ingSin.isNotEmpty()) {
                        Text("SIN UNIDADES", color = RecetasColors.sin, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                        availability.ingSin.forEach { AvailabilityRow(it.name, it.numDisplay, it.unitDisplay, RecetasColors.sin) }
                    }
                }
            }
        }
        item {
            Card(title = "Utensilios") {
                recipe.utensilios.forEach { IngredientRow(it, RecetasColors.accent) }
            }
        }
        if (recipe.prePrep.isNotEmpty()) {
            item { SectionTitle("Pre-preparación") }
            items(recipe.prePrep.size) { i ->
                val st = recipe.prePrep[i]
                StepCard(view, recipe.id, "pre", i, displayNumber = i + 1, text = st.t, title = st.title, time = st.time, rDone = rDone, vm = vm)
            }
        }
        item { SectionTitle("Preparación · ${recipe.tiempo}") }
        items(recipe.prep.size) { i ->
            val st = recipe.prep[i]
            StepCard(view, recipe.id, "prep", i, displayNumber = i + 1 + recipe.prePrep.size, text = st.t, title = st.title, time = st.time, rDone = rDone, vm = vm)
        }
        item {
            Column(Modifier.padding(vertical = 12.dp)) {
                Button(onClick = onEdit, colors = ButtonDefaults.buttonColors(containerColor = RecetasColors.accent), modifier = Modifier.fillMaxWidth()) {
                    Text("✎ Editar receta")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { vm.deleteRecipe(view, recipe.id); onClose() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Eliminar receta", color = RecetasColors.sin)
                }
            }
        }
    }
}

@Composable
private fun StepCard(
    view: String,
    recId: String,
    section: String,
    sectionIndex: Int,
    displayNumber: Int,
    text: String,
    title: String?,
    time: String?,
    rDone: Map<String, Boolean>,
    vm: AppViewModel,
) {
    val id = stepId(view, recId, section, sectionIndex)
    val done = rDone.containsKey(id)
    var editing by remember(id) { mutableStateOf(false) }
    var draft by remember(id) { mutableStateOf(text) }

    Surface(
        color = RecetasColors.card,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, RecetasColors.line),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { vm.toggleStep(id) },
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (done) RecetasColors.accent else RecetasColors.line.copy(alpha = 0.4f),
                    shape = CircleShape,
                    modifier = Modifier.padding(end = 10.dp),
                ) {
                    Text(
                        if (done) "✓" else "$displayNumber",
                        color = if (done) androidx.compose.ui.graphics.Color.White else RecetasColors.inkSoft,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(6.dp),
                    )
                }
                Text(
                    if (!title.isNullOrBlank()) title else "Paso $displayNumber",
                    fontFamily = SerifFamily,
                    fontSize = 17.sp,
                    color = RecetasColors.ink,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { editing = !editing; draft = text }) { Text("🗒", fontSize = 15.sp) }
            }
            if (!editing) {
                Text(
                    text,
                    color = if (done) RecetasColors.inkSoft else RecetasColors.ink.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (!time.isNullOrBlank()) {
                    Surface(color = RecetasColors.accent.copy(alpha = 0.15f), shape = RoundedCornerShape(7.dp), modifier = Modifier.padding(top = 6.dp)) {
                        Text("⏱ $time", color = RecetasColors.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
            } else {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
                Row(Modifier.padding(top = 6.dp)) {
                    Button(onClick = { vm.saveStepText(view, recId, section, sectionIndex, draft.trim()); editing = false }, colors = ButtonDefaults.buttonColors(containerColor = RecetasColors.accent)) {
                        Text("Guardar")
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { editing = false }) { Text("Cancelar") }
                }
            }
        }
    }
}

@Composable
private fun Card(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(color = RecetasColors.card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, RecetasColors.line), modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontFamily = SerifFamily, fontSize = 18.sp, color = RecetasColors.ink, modifier = Modifier.padding(bottom = 8.dp))
            content()
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontFamily = SerifFamily, fontSize = 14.sp, color = RecetasColors.inkSoft, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun IngredientRow(text: String, accent: androidx.compose.ui.graphics.Color) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.layout.Box(Modifier.height(5.dp).padding(end = 8.dp))
        Text("• $text", color = RecetasColors.ink, fontSize = 14.sp)
    }
}

@Composable
private fun AvailabilityRow(name: String, numDisplay: String, unitDisplay: String, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("• $name", color = RecetasColors.ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
        if (numDisplay.isNotEmpty()) {
            Text(numDisplay, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = RecetasColors.ink)
            Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(7.dp), modifier = Modifier.padding(start = 6.dp)) {
                Text(unitDisplay, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
    }
}
