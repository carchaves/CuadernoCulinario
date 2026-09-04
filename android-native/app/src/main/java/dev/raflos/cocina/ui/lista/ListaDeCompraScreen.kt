package dev.raflos.cocina.ui.lista

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.raflos.cocina.data.fmt
import dev.raflos.cocina.data.model.AppState
import dev.raflos.cocina.data.model.Ingredient
import dev.raflos.cocina.data.model.ShoppingList
import dev.raflos.cocina.data.model.Store
import dev.raflos.cocina.ui.AppViewModel
import dev.raflos.cocina.ui.SystemBarsAppearance
import dev.raflos.cocina.ui.theme.JetBrainsMonoFamily
import dev.raflos.cocina.ui.theme.ListaColors
import dev.raflos.cocina.ui.theme.SerifFamily
import kotlin.math.roundToLong

/**
 * Lista de Compra: administrador de listas por comercio (ver data/README.md y el diseño
 * "Cocina App"). Navegación interna en cuatro pasos, todos dentro de esta pantalla para no
 * tocar el `when` de MainActivity: índice → catálogo de artículos → detalle → finalizar compra.
 */
private enum class Paso { INDICE, CATALOGO, DETALLE, CHECKOUT }

@Composable
fun ListaDeCompraScreen(state: AppState, vm: AppViewModel, onBack: () -> Unit) {
    var paso by remember { mutableStateOf(Paso.INDICE) }
    var listaId by remember { mutableStateOf<String?>(null) }
    var nuevaListaAbierta by remember { mutableStateOf(false) }

    // Puede ser null por un instante: crear una lista pasa por `mutate` (asincrónico), así que
    // el id existe antes que el objeto; en ese hueco no se dibuja nada del paso.
    val lista = state.lists.firstOrNull { it.id == listaId }

    SystemBarsAppearance(lightBackground = true)
    BackHandler { if (paso == Paso.INDICE) onBack() else paso = Paso.INDICE }

    Column(
        Modifier
            .fillMaxSize()
            .background(ListaColors.paper)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        val store = lista?.let { l -> state.stores.firstOrNull { it.id == l.storeId } }
        Encabezado(
            backLabel = if (paso == Paso.INDICE) "← Menú" else "← Listas",
            kicker = when (paso) {
                Paso.INDICE -> "Compras"
                Paso.CATALOGO -> "Agregar artículos"
                Paso.DETALLE -> "Lista"
                Paso.CHECKOUT -> "Finalizar compra"
            },
            titulo = if (paso == Paso.INDICE) "Lista de compra" else (store?.name ?: "Lista"),
            progreso = if (paso == Paso.DETALLE && lista != null && lista.items.isNotEmpty()) {
                lista.items.count { it.bought } to lista.items.size
            } else null,
            onBack = { if (paso == Paso.INDICE) onBack() else paso = Paso.INDICE },
        )

        when (paso) {
            Paso.INDICE -> IndiceListas(
                state = state,
                vm = vm,
                onAbrir = { listaId = it; paso = Paso.DETALLE },
                onNueva = { nuevaListaAbierta = true },
            )
            Paso.CATALOGO -> if (lista != null) CatalogoArticulos(state, vm, lista) { paso = Paso.DETALLE }
            Paso.DETALLE -> if (lista != null) DetalleLista(
                state = state,
                vm = vm,
                lista = lista,
                onAgregarArticulos = { paso = Paso.CATALOGO },
                onFinalizar = { paso = Paso.CHECKOUT },
            )
            Paso.CHECKOUT -> if (lista != null) CheckoutLista(state, vm, lista) {
                paso = Paso.INDICE
                listaId = null
            }
        }
    }

    if (nuevaListaAbierta) {
        DialogoNuevaLista(
            state = state,
            vm = vm,
            onCerrar = { nuevaListaAbierta = false },
            onCreada = { id -> nuevaListaAbierta = false; listaId = id; paso = Paso.CATALOGO },
        )
    }
}

// ------------------------------------------------------------------ encabezado

@Composable
private fun Encabezado(
    backLabel: String,
    kicker: String,
    titulo: String,
    progreso: Pair<Int, Int>?,
    onBack: () -> Unit,
) {
    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 12.dp)) {
        TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
            Text(backLabel, color = ListaColors.inkSoft, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(
            kicker.uppercase(),
            color = ListaColors.inkSoft,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.6.sp,
        )
        Text(
            titulo,
            fontFamily = SerifFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            color = ListaColors.ink,
        )
        if (progreso != null) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { if (progreso.second == 0) 0f else progreso.first.toFloat() / progreso.second },
                    color = ListaColors.cart,
                    trackColor = Color(0xFFE7E9DE),
                    modifier = Modifier.weight(1f).height(10.dp),
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    "${progreso.first}/${progreso.second}",
                    fontFamily = SerifFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = ListaColors.ink,
                )
            }
        }
    }
}

// ------------------------------------------------------------------ índice

@Composable
private fun IndiceListas(
    state: AppState,
    vm: AppViewModel,
    onAbrir: (String) -> Unit,
    onNueva: () -> Unit,
) {
    val abiertas = state.lists.filter { it.finalizedAt == null }
    val cerradas = state.lists.filter { it.finalizedAt != null }.reversed()

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        if (abiertas.isEmpty()) {
            item {
                Text(
                    "Todavía no armaste ninguna lista. Creá una eligiendo dónde vas a comprar.",
                    color = ListaColors.inkFaint,
                    fontSize = 13.5.sp,
                    modifier = Modifier.padding(vertical = 22.dp),
                )
            }
        }
        items(abiertas, key = { it.id }) { l ->
            FilaLista(state, l, onAbrir = { onAbrir(l.id) }, onBorrar = { vm.deleteList(l.id) })
        }
        item {
            Button(
                onClick = onNueva,
                colors = ButtonDefaults.buttonColors(containerColor = ListaColors.cart),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            ) {
                Text("+ Nueva lista", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (cerradas.isNotEmpty()) {
            item { SeparadorTitulo("Compras finalizadas") }
            items(cerradas, key = { it.id }) { l ->
                FilaLista(state, l, onAbrir = { onAbrir(l.id) }, onBorrar = { vm.deleteList(l.id) })
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
}

@Composable
private fun FilaLista(state: AppState, l: ShoppingList, onAbrir: () -> Unit, onBorrar: () -> Unit) {
    val store = state.stores.firstOrNull { it.id == l.storeId }
    val subtotal = subtotalDe(state, l)
    Tarjeta(Modifier.padding(vertical = 7.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onAbrir).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(12.dp).background(parseColor(store?.color), CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    store?.name ?: "Sin comercio",
                    fontFamily = SerifFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = if (l.finalizedAt == null) ListaColors.ink else ListaColors.inkSoft,
                )
                store?.address?.takeIf { it.isNotBlank() }?.let {
                    Text(it, fontSize = 11.5.sp, color = ListaColors.inkFaint)
                }
                Text(
                    "${l.items.count { it.bought }}/${l.items.size} artículos" +
                        if (l.finalizedAt != null) " · finalizada" else "",
                    fontSize = 12.sp,
                    color = ListaColors.inkSoft,
                )
            }
            if (subtotal > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        money(subtotal),
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ListaColors.money,
                    )
                    Text("estimado", fontSize = 10.sp, color = ListaColors.inkFaint)
                }
            }
            TextButton(onClick = onBorrar, contentPadding = PaddingValues(6.dp)) {
                Text("✕", color = ListaColors.inkFaint, fontSize = 13.sp)
            }
        }
    }
}

// ------------------------------------------------------------------ nueva lista / comercio

@Composable
private fun DialogoNuevaLista(
    state: AppState,
    vm: AppViewModel,
    onCerrar: () -> Unit,
    onCreada: (String) -> Unit,
) {
    var nombre by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var colorIdx by remember { mutableStateOf(state.stores.size % ListaColors.palette.size) }

    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = ListaColors.card,
        title = { Text("Nueva lista", fontFamily = SerifFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = ListaColors.ink) },
        text = {
            Column {
                Text(
                    "Elegí dónde vas a comprar. La dirección la podés completar después.",
                    fontSize = 13.sp,
                    color = ListaColors.inkSoft,
                )
                state.stores.forEach { st ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onCreada(vm.createList(st.id)) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(11.dp).background(parseColor(st.color), CircleShape))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(st.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = ListaColors.ink)
                            st.address?.takeIf { it.isNotBlank() }?.let {
                                Text(it, fontSize = 11.5.sp, color = ListaColors.inkFaint)
                            }
                        }
                        Text("›", color = ListaColors.inkFaint, fontSize = 15.sp)
                    }
                }
                SeparadorTitulo("Lugar nuevo")
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    placeholder = { Text("Nombre del lugar") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = direccion,
                    onValueChange = { direccion = it },
                    placeholder = { Text("Dirección (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ListaColors.palette.forEachIndexed { i, c ->
                        Box(
                            Modifier
                                .size(if (i == colorIdx) 24.dp else 18.dp)
                                .background(c, CircleShape)
                                .clickable { colorIdx = i },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nombre.isNotBlank()) {
                        val storeId = vm.createStore(nombre, hex(ListaColors.palette[colorIdx]), direccion)
                        onCreada(vm.createList(storeId))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ListaColors.cart),
            ) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar", color = ListaColors.inkSoft) } },
    )
}

// ------------------------------------------------------------------ catálogo de artículos

@Composable
private fun CatalogoArticulos(state: AppState, vm: AppViewModel, lista: ShoppingList, onListo: () -> Unit) {
    // Panel de marcas de boicot abierto (id de ingrediente).
    var panel by remember { mutableStateOf<String?>(null) }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        items(state.pPages, key = { it.id }) { page ->
            val idx = state.pPages.indexOfFirst { it.id == page.id }
            val enLista = page.ingredients.count { ing -> lista.items.any { it.ingredientId == ing.id } }
            Tarjeta(Modifier.padding(vertical = 8.dp)) {
                Column {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).background(ListaColors.palette[idx % ListaColors.palette.size], CircleShape))
                        Spacer(Modifier.width(12.dp))
                        Text(page.name, fontFamily = SerifFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ListaColors.ink, modifier = Modifier.weight(1f))
                        Text("$enLista/${page.ingredients.size}", fontSize = 12.sp, color = ListaColors.inkSoft)
                    }
                    page.ingredients.forEach { ing ->
                        FilaCatalogo(
                            state = state,
                            vm = vm,
                            lista = lista,
                            ing = ing,
                            panelAbierto = panel == ing.id,
                            onTogglePanel = { panel = if (panel == ing.id) null else ing.id },
                        )
                    }
                }
            }
        }
        item {
            Button(
                onClick = onListo,
                colors = ButtonDefaults.buttonColors(containerColor = ListaColors.cart),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            ) { Text("Listo", fontSize = 15.sp, fontWeight = FontWeight.Bold) }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun FilaCatalogo(
    state: AppState,
    vm: AppViewModel,
    lista: ShoppingList,
    ing: Ingredient,
    panelAbierto: Boolean,
    onTogglePanel: () -> Unit,
) {
    val item = lista.items.firstOrNull { it.ingredientId == ing.id }
    val marcas = state.boycottedBrands[ing.id].orEmpty()
    var cantidad by remember(ing.id) { mutableStateOf(item?.quantity?.let { fmt(it, ing.unit) } ?: "") }

    Column(Modifier.padding(horizontal = 12.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(ing.name, fontSize = 15.sp, color = ListaColors.ink, modifier = Modifier.weight(1f))
            if (marcas.isNotEmpty()) {
                Chip("⊘ ${marcas.size}", ListaColors.boycott, ListaColors.boycottSoft)
                Spacer(Modifier.width(6.dp))
            }
            Switch(
                checked = item != null,
                onCheckedChange = { vm.toggleItemInList(lista.id, ing.id) },
                colors = SwitchDefaults.colors(checkedTrackColor = ListaColors.cart),
            )
            TextButton(onClick = onTogglePanel, contentPadding = PaddingValues(6.dp)) {
                Text("⊘", color = if (panelAbierto) ListaColors.boycott else ListaColors.inkFaint, fontSize = 15.sp)
            }
        }
        if (item != null) {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = cantidad,
                    onValueChange = {
                        cantidad = it
                        vm.setItemQuantity(lista.id, ing.id, it.replace(',', '.').toDoubleOrNull(), ing.unit)
                    },
                    placeholder = { Text("Cantidad") },
                    suffix = { Text(ing.unit, color = ListaColors.inkSoft, fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.width(170.dp),
                )
            }
        }
        if (panelAbierto) PanelBoicot(state, vm, ing.id)
    }
}

/** Marcas boicoteadas del ingrediente: se muestran tachadas (mismo criterio que el diseño). */
@Composable
private fun PanelBoicot(state: AppState, vm: AppViewModel, ingId: String) {
    var marca by remember { mutableStateOf("") }
    val marcas = state.boycottedBrands[ingId].orEmpty()

    Surface(
        color = ListaColors.panel,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ListaColors.line),
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "MARCAS DE BOICOT",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = ListaColors.inkFaint,
            )
            marcas.forEach { b ->
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "⊘ $b",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ListaColors.boycott,
                        textDecoration = TextDecoration.LineThrough,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { vm.removeBoycottedBrand(ingId, b) }, contentPadding = PaddingValues(4.dp)) {
                        Text("✕", color = ListaColors.boycott, fontSize = 11.sp)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = marca,
                    onValueChange = { marca = it },
                    placeholder = { Text("Marca a boicotear", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { vm.addBoycottedBrand(ingId, marca); marca = "" },
                    colors = ButtonDefaults.buttonColors(containerColor = ListaColors.boycottSoft, contentColor = ListaColors.boycott),
                ) { Text("Boicotear", fontSize = 13.sp) }
            }
        }
    }
}

// ------------------------------------------------------------------ detalle

@Composable
private fun DetalleLista(
    state: AppState,
    vm: AppViewModel,
    lista: ShoppingList,
    onAgregarArticulos: () -> Unit,
    onFinalizar: () -> Unit,
) {
    var compararAbierto by remember { mutableStateOf(false) }
    val pantry = state.pPages.flatMap { it.ingredients }
    val finalizada = lista.finalizedAt != null

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        if (lista.items.isEmpty()) {
            item {
                Text(
                    "Esta lista está vacía. Agregá artículos para empezar.",
                    color = ListaColors.inkFaint,
                    fontSize = 13.5.sp,
                    modifier = Modifier.padding(vertical = 20.dp),
                )
            }
        }
        items(lista.items, key = { it.ingredientId }) { item ->
            val ing = pantry.firstOrNull { it.id == item.ingredientId }
            val marcas = state.boycottedBrands[item.ingredientId].orEmpty()
            Tarjeta(Modifier.padding(vertical = 5.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            ing?.name ?: "(artículo eliminado)",
                            fontSize = 15.sp,
                            color = if (item.bought) ListaColors.inkFaint else ListaColors.ink,
                            textDecoration = if (item.bought) TextDecoration.LineThrough else null,
                        )
                        marcas.forEach { b ->
                            Text(
                                "⊘ $b",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ListaColors.boycott,
                                textDecoration = TextDecoration.LineThrough,
                            )
                        }
                    }
                    item.quantity?.let { q ->
                        Chip("${fmt(q, item.unit ?: "u")} ${item.unit ?: ""}".trim(), ListaColors.inkSoft, ListaColors.chip)
                        Spacer(Modifier.width(6.dp))
                    }
                    item.price?.let { p ->
                        Text(money(p), fontFamily = JetBrainsMonoFamily, fontSize = 13.sp, color = ListaColors.money)
                        Spacer(Modifier.width(6.dp))
                    }
                    Switch(
                        checked = item.bought,
                        onCheckedChange = { vm.toggleBought(lista.id, item.ingredientId) },
                        enabled = !finalizada,
                        colors = SwitchDefaults.colors(checkedTrackColor = ListaColors.cart),
                    )
                    TextButton(
                        onClick = { vm.removeItemFromList(lista.id, item.ingredientId) },
                        contentPadding = PaddingValues(6.dp),
                    ) { Text("✕", color = ListaColors.inkFaint, fontSize = 13.sp) }
                }
            }
        }
        item {
            val subtotal = subtotalDe(state, lista)
            Tarjeta(Modifier.padding(vertical = 10.dp)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "ESTIMADO",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = ListaColors.inkFaint,
                        modifier = Modifier.weight(1f),
                    )
                    Text(money(subtotal), fontFamily = JetBrainsMonoFamily, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = ListaColors.money)
                }
            }
        }
        if (!finalizada) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onAgregarArticulos,
                        colors = ButtonDefaults.buttonColors(containerColor = ListaColors.card, contentColor = ListaColors.inkSoft),
                        border = BorderStroke(1.dp, ListaColors.line),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    ) { Text("+ Artículos", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                    Button(
                        onClick = { compararAbierto = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ListaColors.card, contentColor = ListaColors.inkSoft),
                        border = BorderStroke(1.dp, ListaColors.line),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                    ) { Text("Comparar", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
            item {
                Button(
                    onClick = onFinalizar,
                    colors = ButtonDefaults.buttonColors(containerColor = ListaColors.cart),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                ) { Text("Finalizar compra", fontSize = 15.sp, fontWeight = FontWeight.Bold) }
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }

    if (compararAbierto) {
        DialogoComparar(state, vm, lista) { compararAbierto = false }
    }
}

/** "Comparar precios": por ingrediente, el precio más barato registrado y el comercio donde
 * está; el botón + lo manda a la lista abierta de ese comercio (creándola si hace falta). */
@Composable
private fun DialogoComparar(state: AppState, vm: AppViewModel, lista: ShoppingList, onCerrar: () -> Unit) {
    val pantry = state.pPages.flatMap { it.ingredients }
    val filas = lista.items.mapNotNull { item ->
        val precios = state.priceHistory[item.ingredientId].orEmpty()
        val mejor = precios.minByOrNull { it.value } ?: return@mapNotNull null
        Triple(pantry.firstOrNull { it.id == item.ingredientId }?.name ?: item.ingredientId, mejor, item.ingredientId)
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        containerColor = ListaColors.card,
        title = { Text("Comparar precios", fontFamily = SerifFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = ListaColors.ink) },
        text = {
            Column {
                if (filas.isEmpty()) {
                    Text(
                        "Todavía no hay precios registrados. Se cargan al finalizar una compra.",
                        fontSize = 13.5.sp,
                        color = ListaColors.inkFaint,
                    )
                }
                filas.forEach { (nombre, mejor, ingId) ->
                    val store = state.stores.firstOrNull { it.id == mejor.key }
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(nombre, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = ListaColors.ink)
                            Text(
                                "${store?.name ?: "?"} · ${money(mejor.value)}",
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 12.sp,
                                color = ListaColors.money,
                            )
                        }
                        if (mejor.key != lista.storeId) {
                            Button(
                                onClick = { vm.addIngredientToStoreList(ingId, mejor.key) },
                                colors = ButtonDefaults.buttonColors(containerColor = ListaColors.cart),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            ) { Text("+", fontSize = 15.sp) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar", color = ListaColors.inkSoft) } },
    )
}

// ------------------------------------------------------------------ finalizar compra

@Composable
private fun CheckoutLista(state: AppState, vm: AppViewModel, lista: ShoppingList, onListo: () -> Unit) {
    val context = LocalContext.current
    val pantry = state.pPages.flatMap { it.ingredients }
    // Precios tipeados a mano, por ingrediente (sin OCR: la foto solo se archiva).
    val precios = remember { mutableStateMapOf<String, String>() }
    var estadoFoto by remember { mutableStateOf("") }
    var subiendo by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
        if (bytes == null) {
            estadoFoto = "No se pudo leer la foto. Podés seguir sin foto."
            return@rememberLauncherForActivityResult
        }
        subiendo = true
        estadoFoto = "Subiendo la foto…"
        vm.uploadReceipt(lista.id, bytes) { ok ->
            subiendo = false
            estadoFoto = if (ok) "Foto del ticket guardada." else "No se pudo subir la foto — podés seguir igual."
        }
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Surface(
                color = ListaColors.panel,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, ListaColors.dashed),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .clickable(enabled = !subiendo) {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Foto del ticket", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = ListaColors.ink)
                    Text(
                        "Se archiva en el repo junto a la compra. Los precios los cargás a mano acá abajo.",
                        fontSize = 12.5.sp,
                        color = ListaColors.inkFaint,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    if (estadoFoto.isNotEmpty()) {
                        Text(estadoFoto, fontSize = 12.sp, color = ListaColors.inkSoft, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
        item {
            Text(
                "PRECIOS PAGADOS",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = ListaColors.inkFaint,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        items(lista.items, key = { it.ingredientId }) { item ->
            val ing = pantry.firstOrNull { it.id == item.ingredientId }
            Tarjeta(Modifier.padding(vertical = 5.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(ing?.name ?: item.ingredientId, fontSize = 15.sp, color = ListaColors.ink, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = precios[item.ingredientId] ?: "",
                        onValueChange = { precios[item.ingredientId] = it },
                        placeholder = { Text("$ 0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.width(130.dp),
                    )
                }
            }
        }
        item {
            Button(
                onClick = {
                    vm.finalizeList(
                        lista.id,
                        lista.items.associate { it.ingredientId to precios[it.ingredientId]?.replace(',', '.')?.toDoubleOrNull() },
                    )
                    onListo()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ListaColors.cart),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            ) { Text("Guardar y cerrar la compra", fontSize = 15.sp, fontWeight = FontWeight.Bold) }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
}

// ------------------------------------------------------------------ piezas reutilizables

@Composable
private fun Tarjeta(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        color = ListaColors.card,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ListaColors.line),
        modifier = modifier.fillMaxWidth(),
    ) { content() }
}

@Composable
private fun Chip(texto: String, color: Color, fondo: Color) {
    Surface(color = fondo, shape = RoundedCornerShape(20.dp)) {
        Text(
            texto,
            color = color,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun SeparadorTitulo(texto: String) {
    Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(ListaColors.line))
        Text(
            texto.uppercase(),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = ListaColors.inkFaint,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
        Box(Modifier.weight(1f).height(1.dp).background(ListaColors.line))
    }
}

/** Subtotal aproximado: precio del ítem si ya se cargó, si no el último precio conocido en ese
 * comercio (× cantidad si la hay). */
private fun subtotalDe(state: AppState, l: ShoppingList): Double = l.items.sumOf { item ->
    val precio = item.price ?: state.priceHistory[item.ingredientId]?.get(l.storeId) ?: 0.0
    precio * (item.quantity?.takeIf { it > 0 } ?: 1.0)
}

private fun money(v: Double): String = "$" + v.roundToLong().toString()

private fun parseColor(hex: String?): Color = try {
    Color(android.graphics.Color.parseColor(hex ?: "#8C8377"))
} catch (e: Exception) {
    ListaColors.inkFaint
}

private fun hex(c: Color): String =
    String.format("#%06X", 0xFFFFFF and android.graphics.Color.argb(
        (c.alpha * 255).toInt(), (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt(),
    ))
