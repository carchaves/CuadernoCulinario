package dev.raflos.cocina.ui.lista

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.raflos.cocina.data.fmt
import dev.raflos.cocina.data.model.AppState
import dev.raflos.cocina.ui.AppViewModel
import dev.raflos.cocina.ui.theme.ListaColors
import dev.raflos.cocina.ui.theme.SerifFamily

@Composable
fun ListaDeCompraScreen(state: AppState, vm: AppViewModel, onBack: () -> Unit) {
    var tab by remember { mutableStateOf(0) } // 0 = Lista, 1 = Artículos

    val allIncluded = state.pPages.flatMap { it.ingredients }.filter { state.lIncluded.containsKey(it.id) }
    val done = allIncluded.count { state.lDone.containsKey(it.id) }
    val total = allIncluded.size

    Column(Modifier.fillMaxSize().background(ListaColors.paper)) {
        Column(Modifier.padding(16.dp)) {
            TextButton(onClick = onBack, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text("← Menú", color = ListaColors.inkSoft, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
            Text("Lista de compra", fontFamily = SerifFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = ListaColors.ink)
            Text("$done/$total en el carrito", color = ListaColors.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
            Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                listOf("Lista" to 0, "Artículos" to 1).forEach { (label, idx) ->
                    Box(
                        Modifier
                            .weight(1f)
                            .background(if (tab == idx) ListaColors.card else Color(0x00000000), RoundedCornerShape(9.dp))
                            .clickable { tab = idx }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, color = if (tab == idx) ListaColors.ink else ListaColors.inkSoft, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }

        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            if (tab == 1) {
                item { AddItemForm(state, vm) }
                items(state.pPages, key = { it.id }) { page ->
                    CategoryCard(title = page.name, color = ListaColors.palette[state.pPages.indexOf(page) % ListaColors.palette.size], countLabel = "${page.ingredients.count { state.lIncluded.containsKey(it.id) }}/${page.ingredients.size}") {
                        page.ingredients.forEach { it2 ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(it2.name, color = ListaColors.ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
                                if (it2.amount > 0) Text("${fmt(it2.amount, it2.unit)} ${it2.unit}", color = ListaColors.inkSoft, fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))
                                Switch(
                                    checked = state.lIncluded.containsKey(it2.id),
                                    onCheckedChange = { vm.toggleIncluded(it2.id) },
                                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF4C7A93)),
                                )
                                IconButton(onClick = { vm.removeIngredient(page.id, it2.id) }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Eliminar", tint = Color(0xFFA9AE9E))
                                }
                            }
                        }
                    }
                }
            } else {
                val categories = state.pPages.mapIndexed { idx, page ->
                    val items = page.ingredients.filter { state.lIncluded.containsKey(it.id) }
                    Triple(page, ListaColors.palette[idx % ListaColors.palette.size], items)
                }.filter { it.third.isNotEmpty() }

                if (categories.isEmpty()) {
                    item { Text("Nada en la lista todavía. Activá \"En lista\" en la pestaña Artículos.", color = ListaColors.inkSoft, fontSize = 13.5.sp, modifier = Modifier.padding(vertical = 20.dp)) }
                }
                items(categories) { (page, color, items) ->
                    CategoryCard(title = page.name, color = color, countLabel = "${items.count { state.lDone.containsKey(it.id) }}/${items.size}") {
                        items.forEach { it2 ->
                            val done2 = state.lDone.containsKey(it2.id)
                            Row(Modifier.fillMaxWidth().clickable { vm.toggleDone(it2.id) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(it2.name, color = if (done2) ListaColors.inkSoft else ListaColors.ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
                                if (it2.amount > 0) Text("${fmt(it2.amount, it2.unit)} ${it2.unit}", color = ListaColors.inkSoft, fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))
                                Switch(checked = done2, onCheckedChange = { vm.toggleDone(it2.id) }, colors = SwitchDefaults.colors(checkedTrackColor = ListaColors.cart))
                                IconButton(onClick = { vm.removeFromLista(it2.id) }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Quitar", tint = Color(0xFFA9AE9E))
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun CategoryCard(title: String, color: androidx.compose.ui.graphics.Color, countLabel: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        color = ListaColors.card,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ListaColors.line),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(12.dp).height(12.dp).background(color, androidx.compose.foundation.shape.CircleShape))
                Spacer(Modifier.width(10.dp))
                Text(title, fontFamily = SerifFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ListaColors.ink, modifier = Modifier.weight(1f))
                Text(countLabel, color = ListaColors.inkSoft, fontSize = 12.sp)
            }
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun AddItemForm(state: AppState, vm: AppViewModel) {
    var name by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("u") }

    Surface(color = ListaColors.card, shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, ListaColors.line), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text("+ Agregar artículo", fontFamily = SerifFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ListaColors.ink)
            OutlinedTextField(value = name, onValueChange = { name = it }, placeholder = { Text("Nombre") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                OutlinedTextField(value = qty, onValueChange = { qty = it }, placeholder = { Text("Cant.") }, modifier = Modifier.weight(1f), singleLine = true)
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val pageId = state.pPages.firstOrNull()?.id
                            if (pageId != null) {
                                vm.addShoppingItem(name.trim(), qty.toDoubleOrNull() ?: 0.0, unit, pageId)
                                name = ""; qty = ""
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ListaColors.cart),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Agregar")
                }
            }
        }
    }
}
