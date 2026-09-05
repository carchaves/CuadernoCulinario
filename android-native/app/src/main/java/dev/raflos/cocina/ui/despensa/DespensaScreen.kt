package dev.raflos.cocina.ui.despensa

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.raflos.cocina.data.fmt
import dev.raflos.cocina.data.model.AppState
import dev.raflos.cocina.data.model.Ingredient
import dev.raflos.cocina.data.model.PantryPage
import dev.raflos.cocina.ui.AppViewModel
import dev.raflos.cocina.ui.SystemBarsAppearance
import dev.raflos.cocina.ui.theme.CartSmallIcon
import dev.raflos.cocina.ui.theme.DespensaColors
import dev.raflos.cocina.ui.theme.FraunceFamily
import dev.raflos.cocina.ui.theme.InterFamily
import dev.raflos.cocina.ui.theme.JetBrainsMonoFamily
import dev.raflos.cocina.ui.theme.PantryIconOptions
import dev.raflos.cocina.ui.theme.SearchLineIcon
import dev.raflos.cocina.ui.theme.StarIcon
import dev.raflos.cocina.ui.theme.pantryIconFor

private val UNITS = listOf("u", "g", "kg", "ml", "L")

/**
 * Navegación en dos niveles como el diseño: el índice de "Repisas" (grilla de a 3 con divisores
 * tipo madera) y la repisa activa aparte. `pActiveId == null` es el índice.
 */
@Composable
fun DespensaScreen(state: AppState, vm: AppViewModel, onBack: () -> Unit) {
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var addingPage by remember { mutableStateOf(false) }
    var newPageName by remember { mutableStateOf("") }
    var iconMenuOpen by remember { mutableStateOf(false) }

    val active = state.pPages.firstOrNull { it.id == state.pActiveId }
    val activeIndex = state.pPages.indexOfFirst { it.id == state.pActiveId }

    SystemBarsAppearance(lightBackground = true)
    // Un paso por vez: desde una repisa, "atrás" vuelve al índice; desde el índice, al Menú.
    BackHandler { if (active != null) vm.setActivePage(null) else onBack() }

    Column(
        Modifier
            .fillMaxSize()
            .background(DespensaColors.paper)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        LinkButton("← Menú", DespensaColors.inkSoft, onClick = onBack)

        // ---- Cabecera ----
        Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 18.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("◧", color = DespensaColors.olive, fontSize = 30.sp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "Despensa",
                            color = DespensaColors.ink,
                            fontFamily = FraunceFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 34.sp,
                        )
                        Text(
                            "Inventario por peso y por unidad",
                            color = DespensaColors.inkSoft,
                            fontFamily = InterFamily,
                            fontSize = 13.5.sp,
                            modifier = Modifier.padding(top = 5.dp),
                        )
                    }
                }
                Box(
                    Modifier
                        .size(40.dp)
                        .background(DespensaColors.card, RoundedCornerShape(11.dp))
                        .border(1.dp, DespensaColors.line, RoundedCornerShape(11.dp))
                        .clickable { searchOpen = !searchOpen; if (!searchOpen) query = "" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (searchOpen) Icons.Filled.Close else SearchLineIcon,
                        contentDescription = "Buscar",
                        tint = DespensaColors.inkSoft,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            if (active != null) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${active.ingredients.size}",
                        color = DespensaColors.olive,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "ítems en esta página",
                        color = DespensaColors.inkFaint,
                        fontFamily = InterFamily,
                        fontSize = 11.5.sp,
                        letterSpacing = 0.3.sp,
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(DespensaColors.ink))

        // ---- Buscador (tarjeta propia) ----
        if (searchOpen) {
            Spacer(Modifier.height(16.dp))
            Surface(
                color = DespensaColors.card,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, DespensaColors.line),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp)) {
                    PlainField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = "Buscar ingrediente en toda la despensa…",
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DespensaColors.paper, RoundedCornerShape(10.dp))
                            .border(1.dp, DespensaColors.line, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                    )
                    val q = query.trim().lowercase()
                    val results = if (q.isEmpty()) emptyList() else state.pPages.flatMap { p ->
                        p.ingredients.filter { it.name.lowercase().contains(q) }.map { it to p }
                    }
                    results.forEach { (ing, page) ->
                        DottedDivider(Modifier.padding(top = 8.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { vm.setActivePage(page.id); searchOpen = false; query = "" }
                                .padding(vertical = 9.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(ing.name, color = DespensaColors.ink, fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                Text(page.name.uppercase(), color = DespensaColors.inkFaint, fontFamily = InterFamily, fontSize = 11.sp, letterSpacing = 0.7.sp)
                            }
                            Text(
                                "${fmt(ing.amount, ing.unit)} ${ing.unit}",
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = DespensaColors.ink,
                            )
                        }
                    }
                    if (q.isNotEmpty() && results.isEmpty()) {
                        Text(
                            "Sin coincidencias.",
                            color = DespensaColors.inkFaint,
                            fontFamily = InterFamily,
                            fontSize = 13.5.sp,
                            modifier = Modifier.padding(top = 12.dp, start = 4.dp),
                        )
                    }
                }
            }
        }

        if (active == null) {
            // ---- Índice de repisas ----
            Surface(
                color = Color(0xFFF5F1E6),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, Color(0xFFC9C2AE)),
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 20.dp),
            ) {
                Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 8.dp)) {
                    state.pPages.chunked(3).forEachIndexed { row, shelf ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            shelf.forEachIndexed { col, page ->
                                ShelfTile(
                                    page = page,
                                    index = row * 3 + col,
                                    modifier = Modifier.weight(1f),
                                    onClick = { vm.setActivePage(page.id) },
                                )
                            }
                            // Relleno para que una fila incompleta no estire los tiles.
                            repeat(3 - shelf.size) { Spacer(Modifier.weight(1f)) }
                        }
                        // Barra "de madera" que separa una repisa de la siguiente.
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 0.dp, bottom = 16.dp)
                                .height(9.dp)
                                .background(
                                    Brush.verticalGradient(listOf(Color(0xFFC9C2AE), Color(0xFFA29B89))),
                                    RoundedCornerShape(2.dp),
                                ),
                        )
                    }

                    if (addingPage) {
                        Row(
                            Modifier
                                .background(DespensaColors.card, RoundedCornerShape(999.dp))
                                .border(1.dp, DespensaColors.olive, RoundedCornerShape(999.dp))
                                .padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PlainField(
                                value = newPageName,
                                onValueChange = { newPageName = it },
                                placeholder = "Nombre de la repisa",
                                fontSize = 13.5.sp,
                                modifier = Modifier.width(150.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .background(DespensaColors.olive, RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (newPageName.isNotBlank()) vm.addPage(newPageName.trim())
                                        newPageName = ""; addingPage = false
                                    },
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Filled.Check, contentDescription = "Crear", tint = Color.White, modifier = Modifier.size(16.dp)) }
                        }
                    } else {
                        Box(
                            Modifier
                                .background(DespensaColors.card, RoundedCornerShape(999.dp))
                                .dashedBorder(DespensaColors.inkFaint, 999.dp)
                                .clickable { addingPage = true }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text("+ Página", color = DespensaColors.olive, fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
            if (state.pPages.isEmpty()) {
                Text(
                    "Creá tu primera repisa.",
                    color = DespensaColors.inkSoft,
                    fontFamily = InterFamily,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            // ---- Repisa activa ----
            LinkButton(
                "← Repisas",
                DespensaColors.inkSoft,
                modifier = Modifier.padding(top = 18.dp, bottom = 12.dp),
                onClick = { vm.setActivePage(null); iconMenuOpen = false },
            )
            Surface(
                color = DespensaColors.card,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, DespensaColors.line),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            ) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            pantryIconFor(active.iconId, activeIndex),
                            contentDescription = null,
                            tint = DespensaColors.olive,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            active.name,
                            fontFamily = FraunceFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 23.sp,
                            color = DespensaColors.ink,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            Modifier
                                .size(28.dp)
                                .background(if (iconMenuOpen) DespensaColors.oliveSoft else Color.Transparent, RoundedCornerShape(7.dp))
                                .clickable { iconMenuOpen = !iconMenuOpen },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                StarIcon,
                                contentDescription = "Cambiar icono",
                                tint = if (iconMenuOpen) DespensaColors.olive else DespensaColors.inkFaint,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Box(
                            Modifier.size(28.dp).clickable { vm.deletePage(active.id) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Eliminar página", tint = DespensaColors.inkFaint, modifier = Modifier.size(16.dp))
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(DespensaColors.line))

                    if (iconMenuOpen) {
                        IconPicker(
                            selectedId = active.iconId,
                            fallbackIndex = activeIndex,
                            onPick = { vm.setPageIcon(active.id, it); iconMenuOpen = false },
                        )
                    }

                    if (active.ingredients.isEmpty()) {
                        Text(
                            "Esta página está vacía. Agrega tu primer ingrediente abajo.",
                            color = DespensaColors.inkFaint,
                            fontFamily = InterFamily,
                            fontSize = 13.5.sp,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                    active.ingredients.forEach { ing ->
                        DottedDivider()
                        IngredientRow(
                            ing = ing,
                            onDec = { vm.adjustIngredient(active.id, ing.id, -1) },
                            onInc = { vm.adjustIngredient(active.id, ing.id, 1) },
                            inLista = state.lIncluded[ing.id] == true,
                            onToLista = { vm.sendIngredientToLista(ing.id) },
                            onRemove = { vm.removeIngredient(active.id, ing.id) },
                        )
                    }

                    Box(Modifier.fillMaxWidth().padding(top = 16.dp).height(2.dp).background(DespensaColors.ink))
                    AddIngredientForm(pageId = active.id, vm = vm)
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

// ---------------------------------------------------------------- piezas reutilizables

@Composable
private fun LinkButton(text: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text.uppercase(),
        color = color,
        fontFamily = InterFamily,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        modifier = modifier.clickable(onClick = onClick),
    )
}

/** Compose no trae divisor punteado; este dibuja el `1px dotted` del diseño. */
@Composable
private fun DottedDivider(modifier: Modifier = Modifier, color: Color = DespensaColors.line, thickness: Float = 2f) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .drawBehind {
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height / 2),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2),
                    strokeWidth = thickness,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(1f, 5f)),
                )
            },
    )
}

/** Borde punteado (la pastilla "+ Página" y demás). */
private fun Modifier.dashedBorder(color: Color, radius: androidx.compose.ui.unit.Dp) = this.drawBehind {
    val r = radius.toPx().coerceAtMost(size.minDimension / 2)
    drawRoundRect(
        color = color,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f)),
        ),
    )
}

@Composable
private fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 15.sp,
    fontFamily: androidx.compose.ui.text.font.FontFamily = InterFamily,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    Box(modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = DespensaColors.ink, fontSize = fontSize, fontFamily = fontFamily),
            cursorBrush = SolidColor(DespensaColors.olive),
            keyboardOptions = keyboardOptions,
            modifier = Modifier.fillMaxWidth(),
        )
        if (value.isEmpty()) {
            Text(placeholder, color = DespensaColors.inkFaint, fontSize = fontSize, fontFamily = fontFamily)
        }
    }
}

@Composable
private fun ShelfTile(page: PantryPage, index: Int, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .heightIn(min = 82.dp)
            .background(DespensaColors.card, RoundedCornerShape(topStart = 11.dp, topEnd = 11.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
            .border(1.dp, DespensaColors.line, RoundedCornerShape(topStart = 11.dp, topEnd = 11.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
    ) {
        Icon(pantryIconFor(page.iconId, index), contentDescription = null, tint = DespensaColors.olive, modifier = Modifier.size(24.dp))
        Text(
            page.name,
            color = DespensaColors.inkSoft,
            fontFamily = InterFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            textAlign = TextAlign.Center,
        )
        Surface(color = DespensaColors.oliveSoft, shape = RoundedCornerShape(999.dp)) {
            Text(
                "${page.ingredients.size}",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.sp,
                color = DespensaColors.olive,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp),
            )
        }
    }
}

@Composable
private fun IconPicker(selectedId: String?, fallbackIndex: Int, onPick: (String) -> Unit) {
    val effective = selectedId ?: PantryIconOptions[fallbackIndex.coerceAtLeast(0) % PantryIconOptions.size].id
    Surface(
        color = DespensaColors.paper,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, DespensaColors.line),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "ICONO DE LA REPISA",
                color = DespensaColors.inkFaint,
                fontFamily = InterFamily,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            PantryIconOptions.chunked(8).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { opt ->
                        val on = opt.id == effective
                        Box(
                            Modifier
                                .weight(1f)
                                .height(34.dp)
                                .background(if (on) DespensaColors.olive else DespensaColors.card, RoundedCornerShape(9.dp))
                                .border(1.dp, if (on) DespensaColors.olive else DespensaColors.line, RoundedCornerShape(9.dp))
                                .clickable { onPick(opt.id) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                opt.icon,
                                contentDescription = opt.id,
                                tint = if (on) DespensaColors.paper else DespensaColors.olive,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    repeat(8 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun IngredientRow(
    ing: Ingredient,
    onDec: () -> Unit,
    onInc: () -> Unit,
    inLista: Boolean,
    onToLista: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 13.dp, horizontal = 2.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            val peso = ing.type == "peso"
            Surface(
                color = if (peso) DespensaColors.oliveSoft else DespensaColors.amberSoft,
                shape = RoundedCornerShape(5.dp),
            ) {
                Text(
                    if (peso) "PESO" else "UNIDAD",
                    color = if (peso) DespensaColors.olive else DespensaColors.amber,
                    fontFamily = InterFamily,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }
            Spacer(Modifier.width(9.dp))
            Text(ing.name, color = DespensaColors.ink, fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 15.5.sp, modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth().padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            StepButton(Icons.Filled.Remove, "Restar", onDec)
            Text(
                "${fmt(ing.amount, ing.unit)} ${ing.unit}",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = DespensaColors.ink,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(horizontal = 6.dp).width(64.dp),
            )
            StepButton(Icons.Filled.Add, "Sumar", onInc)
            // El "hueco punteado" que el diseño usa como flex-spacer.
            DottedDivider(Modifier.weight(1f).padding(horizontal = 11.dp), color = Color(0xFFCDC7B5))
            Box(
                Modifier
                    .size(28.dp)
                    .background(if (inLista) DespensaColors.oliveSoft else Color.Transparent, RoundedCornerShape(7.dp))
                    .clickable(onClick = onToLista),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    CartSmallIcon,
                    contentDescription = "Enviar a lista de compra",
                    tint = if (inLista) DespensaColors.olive else DespensaColors.inkFaint,
                    modifier = Modifier.size(15.dp),
                )
            }
            Box(Modifier.size(28.dp).clickable(onClick = onRemove), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, contentDescription = "Eliminar", tint = DespensaColors.inkFaint, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun StepButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(24.dp)
            .background(DespensaColors.paper, RoundedCornerShape(7.dp))
            .border(1.dp, DespensaColors.line, RoundedCornerShape(7.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = DespensaColors.inkSoft, modifier = Modifier.size(14.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddIngredientForm(pageId: String, vm: AppViewModel) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("g") }
    var unitOpen by remember { mutableStateOf(false) }

    Column(Modifier.padding(top = 16.dp)) {
        PlainField(
            value = name,
            onValueChange = { name = it },
            placeholder = "Nombre del ingrediente",
            modifier = Modifier
                .fillMaxWidth()
                .background(DespensaColors.paper, RoundedCornerShape(9.dp))
                .border(1.dp, DespensaColors.line, RoundedCornerShape(9.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            PlainField(
                value = amount,
                onValueChange = { amount = it },
                placeholder = "0",
                fontFamily = JetBrainsMonoFamily,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .width(88.dp)
                    .background(DespensaColors.paper, RoundedCornerShape(9.dp))
                    .border(1.dp, DespensaColors.line, RoundedCornerShape(9.dp))
                    .padding(horizontal = 11.dp, vertical = 10.dp),
            )
            // La unidad ya distingue peso de unidad ("u"), igual que el `<select>` del diseño.
            ExposedDropdownMenuBox(expanded = unitOpen, onExpandedChange = { unitOpen = it }) {
                Row(
                    Modifier
                        .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                        .background(DespensaColors.card, RoundedCornerShape(9.dp))
                        .border(1.dp, DespensaColors.line, RoundedCornerShape(9.dp))
                        .padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(unit, color = DespensaColors.ink, fontFamily = InterFamily, fontSize = 13.5.sp)
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Unidad", tint = DespensaColors.inkSoft, modifier = Modifier.size(18.dp))
                }
                ExposedDropdownMenu(expanded = unitOpen, onDismissRequest = { unitOpen = false }) {
                    UNITS.forEach { u ->
                        DropdownMenuItem(
                            text = { Text(u, fontFamily = InterFamily, color = DespensaColors.ink) },
                            onClick = { unit = u; unitOpen = false },
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .background(DespensaColors.olive, RoundedCornerShape(9.dp))
                    .clickable {
                        val amt = (amount.replace(',', '.').toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                        if (name.isNotBlank()) {
                            vm.addIngredient(pageId, name.trim(), if (unit == "u") "unidad" else "peso", amt, unit)
                            name = ""; amount = ""
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    "+ Agregar",
                    color = Color.White,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
            }
        }
    }
}
