package dev.raflos.cocina.ui.recetas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import dev.raflos.cocina.data.buildIngString
import dev.raflos.cocina.data.findPantryUnit
import dev.raflos.cocina.data.fmt
import dev.raflos.cocina.data.model.Ingredient
import dev.raflos.cocina.data.model.Recipe
import dev.raflos.cocina.data.model.RecipeStep
import dev.raflos.cocina.data.parseIngToObj
import dev.raflos.cocina.data.roundFor
import dev.raflos.cocina.data.stepFor
import dev.raflos.cocina.ui.AppViewModel
import dev.raflos.cocina.ui.RecipeDraft
import dev.raflos.cocina.ui.theme.DMSansFamily
import dev.raflos.cocina.ui.theme.InstrumentSerifFamily
import dev.raflos.cocina.ui.theme.JetBrainsMonoFamily
import dev.raflos.cocina.ui.theme.RecetasColors

private data class IngDraft(val name: String, val amount: Double, val unit: String)

@Composable
fun RecipeFormSection(
    view: String,
    editingId: String?,
    editingRecipe: Recipe?,
    pantryFlat: List<Ingredient>,
    allRecipes: List<Recipe>,
    vm: AppViewModel,
    onCancel: () -> Unit,
    onSaved: (String) -> Unit,
) {
    var title by remember(editingId) { mutableStateOf(editingRecipe?.title ?: "") }
    var meta by remember(editingId) { mutableStateOf(editingRecipe?.meta ?: emptyList()) }
    var metaInput by remember(editingId) { mutableStateOf("") }
    var tiempo by remember(editingId) { mutableStateOf(editingRecipe?.tiempo?.takeIf { it != "—" } ?: "") }
    var ingredientes by remember(editingId) {
        val initial: List<IngDraft> = editingRecipe?.ingredientes?.map { s ->
            val parsed = parseIngToObj(s, pantryFlat)
            IngDraft(parsed.name, parsed.amount, parsed.unit)
        } ?: emptyList()
        mutableStateOf(initial)
    }
    var ingInput by remember(editingId) { mutableStateOf("") }
    var utensilios by remember(editingId) { mutableStateOf(editingRecipe?.utensilios ?: emptyList()) }
    var utnInput by remember(editingId) { mutableStateOf("") }
    var prePrep by remember(editingId) { mutableStateOf(editingRecipe?.prePrep ?: emptyList()) }
    var prep by remember(editingId) { mutableStateOf(editingRecipe?.prep ?: emptyList()) }

    // Fuentes de autocompletado: la despensa para ingredientes; el resto de las recetas para
    // etiquetas y utensilios (igual que `allUtnSet`/`allMetaSet` del diseño).
    val pantryNames = remember(pantryFlat) { pantryFlat.map { it.name }.distinct().sorted() }
    val allMeta = remember(allRecipes) { allRecipes.flatMap { it.meta }.distinct().sorted() }
    val allUtn = remember(allRecipes) { allRecipes.flatMap { it.utensilios }.distinct().sorted() }

    fun commit() {
        val t = title.trim()
        if (t.isEmpty()) return
        val draft = RecipeDraft(
            title = t,
            meta = meta,
            tiempo = tiempo.trim().ifEmpty { "—" },
            ingredientes = ingredientes.map { buildIngString(it.name, it.amount, it.unit) },
            utensilios = utensilios,
            prePrep = prePrep.filter { it.t.isNotBlank() },
            prep = prep.filter { it.t.isNotBlank() },
        )
        val id = vm.saveRecipe(view, editingId, draft)
        onSaved(id)
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            TextButton(onClick = onCancel, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text("← RECETAS", color = RecetasColors.inkSoft, fontFamily = DMSansFamily, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
            }
        }
        item {
            FormCard("Información") {
                OutlinedTextField(value = title, onValueChange = { title = it }, placeholder = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                ChipInput(items = meta, input = metaInput, suggestions = allMeta, placeholder = "Buscar o agregar etiqueta", onInputChange = { metaInput = it }, onAdd = { v -> if (v.isNotBlank() && v !in meta) meta = meta + v.trim(); metaInput = "" }, onRemove = { v -> meta = meta - v })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = tiempo, onValueChange = { tiempo = it }, placeholder = { Text("Tiempo estimado (ej: 30 min)") }, modifier = Modifier.fillMaxWidth())
            }
        }
        item {
            FormCard("Ingredientes") {
                ingredientes.forEachIndexed { i, ing ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(ing.name, color = RecetasColors.ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { ingredientes = ingredientes.toMutableList().also { it[i] = ing.copy(amount = roundFor(ing.amount - stepFor(ing.unit), ing.unit)) } }) {
                            Icon(androidx.compose.material.icons.Icons.Filled.KeyboardArrowDown, contentDescription = "Restar", tint = RecetasColors.inkSoft)
                        }
                        Text(fmt(ing.amount, ing.unit), fontFamily = JetBrainsMonoFamily, fontSize = 13.sp, color = RecetasColors.ink)
                        IconButton(onClick = { ingredientes = ingredientes.toMutableList().also { it[i] = ing.copy(amount = roundFor(ing.amount + stepFor(ing.unit), ing.unit)) } }) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Sumar", tint = RecetasColors.inkSoft)
                        }
                        Text(ing.unit, fontSize = 11.sp, color = RecetasColors.inkSoft, modifier = Modifier.padding(end = 6.dp))
                        IconButton(onClick = { ingredientes = ingredientes.filterIndexed { idx, _ -> idx != i } }) {
                            Icon(Icons.Filled.Close, contentDescription = "Quitar", tint = RecetasColors.inkSoft)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    val addIng: (String) -> Unit = { raw ->
                        val name = raw.trim()
                        if (name.isNotEmpty() && ingredientes.none { it.name == name }) {
                            ingredientes = ingredientes + IngDraft(name, 0.0, findPantryUnit(pantryFlat, name))
                            vm.addIngredientToPantry(name)
                        }
                        ingInput = ""
                    }
                    Box(Modifier.weight(1f)) {
                        OutlinedTextField(value = ingInput, onValueChange = { ingInput = it }, placeholder = { Text("Buscar en despensa o agregar ingrediente", fontFamily = DMSansFamily) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Autocomplete(ingInput, pantryNames.filterNot { n -> ingredientes.any { it.name == n } }, addIng)
                    }
                    IconButton(onClick = {
                        addIng(ingInput)
                    }) { Icon(Icons.Filled.Add, contentDescription = "Agregar", tint = RecetasColors.accent) }
                }
            }
        }
        item {
            FormCard("Utensilios") {
                ChipInput(items = utensilios, input = utnInput, suggestions = allUtn, placeholder = "Buscar o agregar utensilio", onInputChange = { utnInput = it }, onAdd = { v -> if (v.isNotBlank() && v !in utensilios) utensilios = utensilios + v.trim(); utnInput = "" }, onRemove = { v -> utensilios = utensilios - v })
            }
        }
        item {
            StepsEditorSection("Pre-preparación", prePrep, offset = 0, onChange = { prePrep = it })
        }
        item {
            StepsEditorSection("Preparación", prep, offset = prePrep.size, onChange = { prep = it })
        }
        item {
            Column(Modifier.padding(vertical = 16.dp)) {
                Row {
                    Button(onClick = ::commit, colors = ButtonDefaults.buttonColors(containerColor = RecetasColors.accent), modifier = Modifier.weight(1f)) {
                        Text(if (editingId != null) "Guardar cambios" else "Guardar receta")
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                }
            }
        }
    }
}

@Composable
private fun FormCard(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(color = RecetasColors.card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, RecetasColors.line), modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontFamily = InstrumentSerifFamily, fontSize = 20.sp, color = RecetasColors.ink, modifier = Modifier.padding(bottom = 8.dp))
            content()
        }
    }
}

@Composable
private fun ChipInput(
    items: List<String>,
    input: String,
    suggestions: List<String>,
    placeholder: String,
    onInputChange: (String) -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    Column {
        if (items.isNotEmpty()) {
            Row(Modifier.padding(bottom = 6.dp)) {
                items.forEach { tag ->
                    Surface(color = RecetasColors.line.copy(alpha = 0.4f), shape = RoundedCornerShape(999.dp), modifier = Modifier.padding(end = 6.dp)) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(tag, fontSize = 12.sp, color = RecetasColors.inkSoft)
                            IconButton(onClick = { onRemove(tag) }, modifier = Modifier.padding(start = 2.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Quitar", tint = RecetasColors.inkSoft, modifier = Modifier.width(14.dp))
                            }
                        }
                    }
                }
            }
        }
        Row {
            Box(Modifier.weight(1f)) {
                OutlinedTextField(value = input, onValueChange = onInputChange, placeholder = { Text(placeholder, fontFamily = DMSansFamily) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Autocomplete(input, suggestions.filterNot { it in items }, onAdd)
            }
            IconButton(onClick = { onAdd(input) }) { Icon(Icons.Filled.Add, contentDescription = "Agregar", tint = RecetasColors.accent) }
        }
    }
}

@Composable
private fun StepsEditorSection(title: String, steps: List<RecipeStep>, offset: Int, onChange: (List<RecipeStep>) -> Unit) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(title, fontFamily = InstrumentSerifFamily, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 15.sp, color = RecetasColors.inkSoft, modifier = Modifier.padding(bottom = 8.dp))
        steps.forEachIndexed { i, step ->
            Surface(color = RecetasColors.card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, RecetasColors.line), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${offset + i + 1}",
                            color = RecetasColors.accent,
                            fontFamily = InstrumentSerifFamily,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontSize = 24.sp,
                            modifier = Modifier.width(26.dp),
                        )
                        // Título del paso, editable (antes solo se mostraba "Paso N").
                        androidx.compose.foundation.text.BasicTextField(
                            value = step.title ?: "",
                            onValueChange = { v -> onChange(steps.toMutableList().also { it[i] = step.copy(title = v.ifBlank { null }) }) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = RecetasColors.ink,
                                fontFamily = InstrumentSerifFamily,
                                fontSize = 20.sp,
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(RecetasColors.accent),
                            decorationBox = { inner ->
                                if (step.title.isNullOrEmpty()) {
                                    Text("Paso ${offset + i + 1}", color = RecetasColors.inkSoft, fontFamily = InstrumentSerifFamily, fontSize = 20.sp)
                                }
                                inner()
                            },
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { if (i > 0) onChange(steps.toMutableList().apply { add(i - 1, removeAt(i)) }) }, enabled = i > 0) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Subir", tint = RecetasColors.inkSoft)
                        }
                        IconButton(onClick = { if (i < steps.size - 1) onChange(steps.toMutableList().apply { add(i + 1, removeAt(i)) }) }, enabled = i < steps.size - 1) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Bajar", tint = RecetasColors.inkSoft)
                        }
                        IconButton(onClick = { onChange(steps.filterIndexed { idx, _ -> idx != i }) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Quitar", tint = RecetasColors.inkSoft)
                        }
                    }
                    OutlinedTextField(
                        value = step.t,
                        onValueChange = { text -> onChange(steps.toMutableList().also { it[i] = step.copy(t = text) }) },
                        placeholder = { Text("Descripción del paso", fontFamily = DMSansFamily) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        OutlinedButton(onClick = { onChange(steps + RecipeStep(t = "")) }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text("+ Agregar paso", color = RecetasColors.accent)
        }
    }
}

/**
 * Autocompletado simple: filtra por `contains` sobre los valores ya existentes y ofrece
 * `+ Agregar "X"` cuando lo tipeado no está en la lista (igual que el diseño).
 */
@Composable
private fun Autocomplete(input: String, options: List<String>, onPick: (String) -> Unit) {
    val q = input.trim()
    val matches = if (q.isEmpty()) emptyList() else options.filter { it.contains(q, ignoreCase = true) }.take(8)
    val showAddNew = q.isNotEmpty() && options.none { it.equals(q, ignoreCase = true) }
    DropdownMenu(
        expanded = matches.isNotEmpty() || showAddNew,
        onDismissRequest = {},
        properties = androidx.compose.ui.window.PopupProperties(focusable = false),
    ) {
        matches.forEach { opt ->
            DropdownMenuItem(
                text = { Text(opt, fontFamily = DMSansFamily, fontSize = 13.sp, color = RecetasColors.ink) },
                onClick = { onPick(opt) },
            )
        }
        if (showAddNew) {
            DropdownMenuItem(
                text = { Text("+ Agregar \"$q\"", fontFamily = DMSansFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = RecetasColors.accent) },
                onClick = { onPick(q) },
            )
        }
    }
}
