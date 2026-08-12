package dev.raflos.cocina.ui.despensa

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.raflos.cocina.data.fmt
import dev.raflos.cocina.data.model.AppState
import dev.raflos.cocina.data.model.PantryPage
import dev.raflos.cocina.ui.AppViewModel
import dev.raflos.cocina.ui.SystemBarsAppearance
import dev.raflos.cocina.ui.theme.DespensaColors
import dev.raflos.cocina.ui.theme.SerifFamily

@Composable
fun DespensaScreen(state: AppState, vm: AppViewModel, onBack: () -> Unit) {
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var addingPage by remember { mutableStateOf(false) }
    var newPageName by remember { mutableStateOf("") }

    val active = state.pPages.firstOrNull { it.id == state.pActiveId }

    SystemBarsAppearance(lightBackground = true)
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(DespensaColors.paper).windowInsetsPadding(WindowInsets.systemBars)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            TextButton(onClick = onBack, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text("← Menú", color = DespensaColors.inkSoft, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Despensa", color = DespensaColors.ink, fontFamily = SerifFamily, fontWeight = FontWeight.Bold, fontSize = 30.sp)
                    Text("Inventario por peso y por unidad", color = DespensaColors.inkSoft, fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { searchOpen = !searchOpen; if (!searchOpen) query = "" }) {
                        Icon(if (searchOpen) Icons.Filled.Close else Icons.Filled.Search, contentDescription = "Buscar", tint = DespensaColors.inkSoft)
                    }
                    if (active != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${active.ingredients.size}", color = DespensaColors.olive, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            Text("ítems", color = DespensaColors.inkFaint, fontSize = 10.sp)
                        }
                    }
                }
            }

            if (searchOpen) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Buscar en toda la despensa…") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                val q = query.trim().lowercase()
                if (q.isNotEmpty()) {
                    val results = state.pPages.flatMap { p -> p.ingredients.filter { it.name.lowercase().contains(q) }.map { it to p } }
                    Column(Modifier.padding(top = 6.dp)) {
                        results.forEach { (ing, page) ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { vm.setActivePage(page.id); searchOpen = false; query = "" }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column {
                                    Text(ing.name, color = DespensaColors.ink, fontSize = 15.sp)
                                    Text(page.name.uppercase(), color = DespensaColors.inkFaint, fontSize = 10.sp)
                                }
                                Text("${fmt(ing.amount, ing.unit)} ${ing.unit}", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = DespensaColors.ink)
                            }
                        }
                        if (results.isEmpty()) Text("Sin coincidencias.", color = DespensaColors.inkFaint, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(state.pPages) { page -> PageTab(page, page.id == state.pActiveId) { vm.setActivePage(page.id) } }
                item {
                    if (addingPage) {
                        Row(
                            Modifier.background(DespensaColors.card, RoundedCornerShape(999.dp)).border(1.dp, DespensaColors.olive, RoundedCornerShape(999.dp)).padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = newPageName,
                                onValueChange = { newPageName = it },
                                modifier = Modifier.width(140.dp),
                                singleLine = true,
                                placeholder = { Text("Nombre", fontSize = 13.sp) },
                            )
                            IconButton(onClick = {
                                if (newPageName.isNotBlank()) vm.addPage(newPageName.trim())
                                newPageName = ""; addingPage = false
                            }) { Icon(Icons.Filled.Add, contentDescription = "Crear", tint = DespensaColors.olive) }
                        }
                    } else {
                        TextButton(onClick = { addingPage = true }) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = DespensaColors.olive, modifier = Modifier.size(16.dp))
                            Text(" Página", color = DespensaColors.olive, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                        }
                    }
                }
            }
        }

        if (active == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Creá tu primera página de despensa.", color = DespensaColors.inkSoft)
            }
        } else {
            Surface(
                color = DespensaColors.card,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DespensaColors.line),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(active.name, fontFamily = SerifFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = DespensaColors.ink)
                        IconButton(onClick = { vm.deletePage(active.id) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Eliminar página", tint = DespensaColors.inkFaint)
                        }
                    }
                    LazyColumn(Modifier.weight(1f, fill = false)) {
                        items(active.ingredients, key = { it.id }) { ing ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = if (ing.type == "peso") DespensaColors.oliveSoft else DespensaColors.amberSoft,
                                        shape = RoundedCornerShape(4.dp),
                                    ) {
                                        Text(
                                            ing.type,
                                            color = if (ing.type == "peso") DespensaColors.olive else DespensaColors.amber,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(ing.name, color = DespensaColors.ink, fontSize = 15.5.sp, modifier = Modifier.weight(1f))
                                }
                                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        StepButton(Icons.Filled.Remove) { vm.adjustIngredient(active.id, ing.id, -1) }
                                        Text(
                                            "${fmt(ing.amount, ing.unit)} ${ing.unit}",
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold,
                                            color = DespensaColors.ink,
                                            modifier = Modifier.padding(horizontal = 8.dp),
                                        )
                                        StepButton(Icons.Filled.Add) { vm.adjustIngredient(active.id, ing.id, 1) }
                                    }
                                    IconButton(onClick = { vm.removeIngredient(active.id, ing.id) }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Eliminar", tint = DespensaColors.inkFaint, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        if (active.ingredients.isEmpty()) {
                            item { Text("Esta página está vacía.", color = DespensaColors.inkFaint, fontSize = 13.5.sp, modifier = Modifier.padding(vertical = 12.dp)) }
                        }
                    }
                    AddIngredientForm(pageId = active.id, vm = vm)
                }
            }
        }
    }
}

@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .border(1.dp, DespensaColors.line, RoundedCornerShape(7.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = DespensaColors.inkSoft, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun PageTab(page: PantryPage, active: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .background(if (active) DespensaColors.ink else DespensaColors.card, RoundedCornerShape(999.dp))
            .border(1.dp, if (active) DespensaColors.ink else DespensaColors.line, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(page.name, color = if (active) DespensaColors.paper else DespensaColors.inkSoft, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(6.dp))
        Surface(color = if (active) DespensaColors.olive.copy(alpha = 0.3f) else DespensaColors.oliveSoft, shape = RoundedCornerShape(999.dp)) {
            Text(
                "${page.ingredients.size}",
                fontSize = 11.sp,
                color = if (active) DespensaColors.paper else DespensaColors.olive,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
    }
}

@Composable
private fun AddIngredientForm(pageId: String, vm: AppViewModel) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("peso") }
    var amount by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("g") }

    Column(Modifier.padding(top = 12.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("Nombre del ingrediente") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("peso", "unidad").forEach { t ->
                TextButton(onClick = { type = t; if (t == "unidad") unit = "u" else unit = "g" }) {
                    Text(if (t == "peso") "Peso" else "Unidad", color = if (type == t) DespensaColors.olive else DespensaColors.inkSoft, fontWeight = if (type == t) FontWeight.Bold else FontWeight.Normal)
                }
            }
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                placeholder = { Text("0") },
                modifier = Modifier.width(90.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            if (type == "peso") {
                listOf("g", "kg", "ml", "L").forEach { u ->
                    TextButton(onClick = { unit = u }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)) {
                        Text(u, color = if (unit == u) DespensaColors.olive else DespensaColors.inkFaint, fontWeight = if (unit == u) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
        Button(
            onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                if (name.isNotBlank()) {
                    vm.addIngredient(pageId, name.trim(), type, amt.coerceAtLeast(0.0), if (type == "unidad") "u" else unit)
                    name = ""; amount = ""
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DespensaColors.olive),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(" Agregar")
        }
    }
}
