package dev.raflos.cocina.ui.menaje

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
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
import dev.raflos.cocina.ui.theme.InterFamily
import dev.raflos.cocina.ui.theme.JetBrainsMonoFamily
import dev.raflos.cocina.ui.theme.MenajeColors
import dev.raflos.cocina.ui.theme.PantryIconOptions
import dev.raflos.cocina.ui.theme.SearchLineIcon
import dev.raflos.cocina.ui.theme.StarIcon
import dev.raflos.cocina.ui.theme.pantryIconFor

private val UNITS = listOf("u", "g", "kg", "ml", "L")

/**
 * Menaje: misma estructura que Despensa (ver `ui/despensa/DespensaScreen.kt`), pero sobre el
 * inventario de utensilios. Se mantiene como pantalla paralela a propósito, para no tocar la
 * Despensa ya en uso.
 *
 * Navegación en dos niveles como el diseño: el índice de "Cajones" (grilla de a 3) y el
 * cajón activo aparte. `mActiveId == null` es el índice.
 */
@Composable
fun MenajeScreen(state: AppState, vm: AppViewModel, onBack: () -> Unit) {
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var addingPage by remember { mutableStateOf(false) }
    var newPageName by remember { mutableStateOf("") }
    var iconMenuOpen by remember { mutableStateOf(false) }

    val active = state.mPages.firstOrNull { it.id == state.mActiveId }
    val activeIndex = state.mPages.indexOfFirst { it.id == state.mActiveId }

    SystemBarsAppearance(lightBackground = true)
    // Un paso por vez: desde un cajón, "atrás" vuelve al índice; desde el índice, al Menú.
    BackHandler { if (active != null) vm.setActiveMenajePage(null) else onBack() }

    Column(
        Modifier
            .fillMaxSize()
            .background(MenajeColors.paper)
            .windowInsetsPadding(WindowInsets.systemBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        LinkButton("← Menú", MenajeColors.inkSoft, onClick = onBack)

        // ---- Cabecera ----
        Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 18.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("◧", color = MenajeColors.steel, fontSize = 30.sp)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "MENAJE",
                            color = MenajeColors.ink,
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 30.sp,
                            letterSpacing = 0.5.sp,
                        )
                        Text(
                            "Inventario de utensilios y equipamiento",
                            color = MenajeColors.inkSoft,
                            fontFamily = InterFamily,
                            fontSize = 13.5.sp,
                            modifier = Modifier.padding(top = 5.dp),
                        )
                    }
                }
                Box(
                    Modifier
                        .size(40.dp)
                        .background(MenajeColors.card, RoundedCornerShape(11.dp))
                        .border(1.dp, MenajeColors.line, RoundedCornerShape(11.dp))
                        .clickable { searchOpen = !searchOpen; if (!searchOpen) query = "" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (searchOpen) Icons.Filled.Close else SearchLineIcon,
                        contentDescription = "Buscar",
                        tint = MenajeColors.inkSoft,
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
                        color = MenajeColors.copper,
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        "ítems en este cajón",
                        color = MenajeColors.inkFaint,
                        fontFamily = InterFamily,
                        fontSize = 11.5.sp,
                        letterSpacing = 0.3.sp,
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(MenajeColors.ink))

        // ---- Buscador (tarjeta propia) ----
        if (searchOpen) {
            Spacer(Modifier.height(16.dp))
            Surface(
                color = MenajeColors.card,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MenajeColors.line),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp)) {
                    PlainField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = "Buscar utensilio en todo el menaje…",
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MenajeColors.paper, RoundedCornerShape(10.dp))
                            .border(1.dp, MenajeColors.line, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                    )
                    val q = query.trim().lowercase()
                    val results = if (q.isEmpty()) emptyList() else state.mPages.flatMap { p ->
                        p.ingredients.filter { it.name.lowercase().contains(q) }.map { it to p }
                    }
                    results.forEach { (ing, page) ->
                        RowDivider(Modifier.padding(top = 8.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { vm.setActiveMenajePage(page.id); searchOpen = false; query = "" }
                                .padding(vertical = 9.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(ing.name, color = MenajeColors.ink, fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                Text(page.name.uppercase(), color = MenajeColors.inkFaint, fontFamily = InterFamily, fontSize = 11.sp, letterSpacing = 0.7.sp)
                            }
                            Text(
                                "${fmt(ing.amount, ing.unit)} ${ing.unit}",
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = MenajeColors.copper,
                            )
                        }
                    }
                    if (q.isNotEmpty() && results.isEmpty()) {
                        Text(
                            "Sin coincidencias.",
                            color = MenajeColors.inkFaint,
                            fontFamily = InterFamily,
                            fontSize = 13.5.sp,
                            modifier = Modifier.padding(top = 12.dp, start = 4.dp),
                        )
                    }
                }
            }
        }

        if (active == null) {
            // ---- Índice de cajones ----
            Surface(
                color = MenajeColors.steelSoft,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, MenajeColors.line),
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 20.dp),
            ) {
                Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 8.dp)) {
                    state.mPages.chunked(3).forEachIndexed { row, shelf ->
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            shelf.forEachIndexed { col, page ->
                                ShelfTile(
                                    page = page,
                                    index = row * 3 + col,
                                    modifier = Modifier.weight(1f),
                                    onClick = { vm.setActiveMenajePage(page.id) },
                                )
                            }
                            // Relleno para que una fila incompleta no estire los tiles.
                            repeat(3 - shelf.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }

                    if (addingPage) {
                        Row(
                            Modifier
                                .background(MenajeColors.card, RoundedCornerShape(999.dp))
                                .border(1.dp, MenajeColors.steel, RoundedCornerShape(999.dp))
                                .padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PlainField(
                                value = newPageName,
                                onValueChange = { newPageName = it },
                                placeholder = "Nombre del cajón",
                                fontSize = 13.5.sp,
                                modifier = Modifier.width(150.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .background(MenajeColors.copper, RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (newPageName.isNotBlank()) vm.addMenajePage(newPageName.trim())
                                        newPageName = ""; addingPage = false
                                    },
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Filled.Check, contentDescription = "Crear", tint = Color.White, modifier = Modifier.size(16.dp)) }
                        }
                    } else {
                        Box(
                            Modifier
                                .background(MenajeColors.card, RoundedCornerShape(999.dp))
                                .dashedBorder(MenajeColors.steel, 999.dp)
                                .clickable { addingPage = true }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text("+ Cajón", color = MenajeColors.steel, fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
            if (state.mPages.isEmpty()) {
                Text(
                    "Creá tu primer cajón.",
                    color = MenajeColors.inkSoft,
                    fontFamily = InterFamily,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            // ---- Cajón activo ----
            LinkButton(
                "← Cajones",
                MenajeColors.inkSoft,
                modifier = Modifier.padding(top = 18.dp, bottom = 12.dp),
                onClick = { vm.setActiveMenajePage(null); iconMenuOpen = false },
            )
            Surface(
                color = MenajeColors.card,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MenajeColors.line),
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
                            tint = MenajeColors.steel,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            active.name.uppercase(),
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            letterSpacing = 0.5.sp,
                            color = MenajeColors.ink,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            Modifier
                                .size(28.dp)
                                .background(if (iconMenuOpen) MenajeColors.steelSoft else Color.Transparent, RoundedCornerShape(7.dp))
                                .clickable { iconMenuOpen = !iconMenuOpen },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                StarIcon,
                                contentDescription = "Cambiar icono",
                                tint = if (iconMenuOpen) MenajeColors.steel else MenajeColors.inkFaint,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Box(
                            Modifier.size(28.dp).clickable { vm.deleteMenajePage(active.id) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Eliminar cajón", tint = MenajeColors.inkFaint, modifier = Modifier.size(16.dp))
                        }
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(MenajeColors.line))

                    if (iconMenuOpen) {
                        IconPicker(
                            selectedId = active.iconId,
                            fallbackIndex = activeIndex,
                            onPick = { vm.setMenajePageIcon(active.id, it); iconMenuOpen = false },
                        )
                    }

                    if (active.ingredients.isEmpty()) {
                        Text(
                            "Este cajón está vacío. Agrega tu primer utensilio abajo.",
                            color = MenajeColors.inkFaint,
                            fontFamily = InterFamily,
                            fontSize = 13.5.sp,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                    active.ingredients.forEach { ing ->
                        RowDivider()
                        ItemRow(
                            ing = ing,
                            onDec = { vm.adjustMenajeItem(active.id, ing.id, -1) },
                            onInc = { vm.adjustMenajeItem(active.id, ing.id, 1) },
                            onRemove = { vm.removeMenajeItem(active.id, ing.id) },
                        )
                    }

                    Box(Modifier.fillMaxWidth().padding(top = 16.dp).height(2.dp).background(MenajeColors.ink))
                    AddItemForm(pageId = active.id, vm = vm)
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

/** Separador liso de 1dp: en Menaje la lectura es "cajón de herramientas", no ficha de papel. */
@Composable
private fun RowDivider(modifier: Modifier = Modifier, color: Color = MenajeColors.line) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color))
}

/** Borde punteado (la pastilla "+ Cajón" y demás). */
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
            textStyle = TextStyle(color = MenajeColors.ink, fontSize = fontSize, fontFamily = fontFamily),
            cursorBrush = SolidColor(MenajeColors.steel),
            keyboardOptions = keyboardOptions,
            modifier = Modifier.fillMaxWidth(),
        )
        if (value.isEmpty()) {
            Text(placeholder, color = MenajeColors.inkFaint, fontSize = fontSize, fontFamily = fontFamily)
        }
    }
}

@Composable
private fun ShelfTile(page: PantryPage, index: Int, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(9.dp)
    Column(
        modifier
            .heightIn(min = 92.dp)
            .background(MenajeColors.card, shape)
            .border(1.dp, MenajeColors.line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
    ) {
        // "Manija" del cajón: barrita de cobre centrada arriba del icono.
        Box(Modifier.width(28.dp).height(4.dp).background(MenajeColors.copper, RoundedCornerShape(2.dp)))
        Icon(pantryIconFor(page.iconId, index), contentDescription = null, tint = MenajeColors.steel, modifier = Modifier.size(24.dp))
        Text(
            page.name,
            color = MenajeColors.inkSoft,
            fontFamily = InterFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            textAlign = TextAlign.Center,
        )
        Surface(color = MenajeColors.copperSoft, shape = RoundedCornerShape(999.dp)) {
            Text(
                "${page.ingredients.size}",
                fontFamily = JetBrainsMonoFamily,
                fontSize = 11.sp,
                color = MenajeColors.copper,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp),
            )
        }
    }
}

@Composable
private fun IconPicker(selectedId: String?, fallbackIndex: Int, onPick: (String) -> Unit) {
    val effective = selectedId ?: PantryIconOptions[fallbackIndex.coerceAtLeast(0) % PantryIconOptions.size].id
    Surface(
        color = MenajeColors.paper,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MenajeColors.line),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "ICONO DEL CAJÓN",
                color = MenajeColors.inkFaint,
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
                                .background(if (on) MenajeColors.steel else MenajeColors.card, RoundedCornerShape(9.dp))
                                .border(1.dp, if (on) MenajeColors.steel else MenajeColors.line, RoundedCornerShape(9.dp))
                                .clickable { onPick(opt.id) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                opt.icon,
                                contentDescription = opt.id,
                                tint = if (on) MenajeColors.paper else MenajeColors.steel,
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
private fun ItemRow(
    ing: Ingredient,
    onDec: () -> Unit,
    onInc: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 13.dp, horizontal = 2.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            val peso = ing.type == "peso"
            Surface(
                color = if (peso) MenajeColors.steelSoft else MenajeColors.copperSoft,
                shape = RoundedCornerShape(5.dp),
            ) {
                Text(
                    if (peso) "PESO" else "UNIDAD",
                    color = if (peso) MenajeColors.steel else MenajeColors.copper,
                    fontFamily = InterFamily,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }
            Spacer(Modifier.width(9.dp))
            Text(ing.name, color = MenajeColors.ink, fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 15.5.sp, modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth().padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            StepButton(Icons.Filled.Remove, "Restar", onDec)
            Text(
                "${fmt(ing.amount, ing.unit)} ${ing.unit}",
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MenajeColors.copper,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(horizontal = 6.dp).width(64.dp),
            )
            StepButton(Icons.Filled.Add, "Sumar", onInc)
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(28.dp).clickable(onClick = onRemove), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, contentDescription = "Eliminar", tint = MenajeColors.inkFaint, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun StepButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(24.dp)
            .background(MenajeColors.paper, RoundedCornerShape(7.dp))
            .border(1.dp, MenajeColors.line, RoundedCornerShape(7.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = MenajeColors.inkSoft, modifier = Modifier.size(14.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddItemForm(pageId: String, vm: AppViewModel) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    // En menaje casi todo se cuenta por unidad; igual se puede elegir peso.
    var unit by remember { mutableStateOf("u") }
    var unitOpen by remember { mutableStateOf(false) }

    Column(Modifier.padding(top = 16.dp)) {
        PlainField(
            value = name,
            onValueChange = { name = it },
            placeholder = "Nombre del utensilio",
            modifier = Modifier
                .fillMaxWidth()
                .background(MenajeColors.paper, RoundedCornerShape(9.dp))
                .border(1.dp, MenajeColors.line, RoundedCornerShape(9.dp))
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
                    .background(MenajeColors.paper, RoundedCornerShape(9.dp))
                    .border(1.dp, MenajeColors.line, RoundedCornerShape(9.dp))
                    .padding(horizontal = 11.dp, vertical = 10.dp),
            )
            // La unidad ya distingue peso de unidad ("u"), igual que el `<select>` del diseño.
            ExposedDropdownMenuBox(expanded = unitOpen, onExpandedChange = { unitOpen = it }) {
                Row(
                    Modifier
                        .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                        .background(MenajeColors.card, RoundedCornerShape(9.dp))
                        .border(1.dp, MenajeColors.line, RoundedCornerShape(9.dp))
                        .padding(start = 12.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(unit, color = MenajeColors.ink, fontFamily = InterFamily, fontSize = 13.5.sp)
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Unidad", tint = MenajeColors.inkSoft, modifier = Modifier.size(18.dp))
                }
                ExposedDropdownMenu(expanded = unitOpen, onDismissRequest = { unitOpen = false }) {
                    UNITS.forEach { u ->
                        DropdownMenuItem(
                            text = { Text(u, fontFamily = InterFamily, color = MenajeColors.ink) },
                            onClick = { unit = u; unitOpen = false },
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .background(MenajeColors.steel, RoundedCornerShape(9.dp))
                    .clickable {
                        val amt = (amount.replace(',', '.').toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                        if (name.isNotBlank()) {
                            vm.addMenajeItem(pageId, name.trim(), if (unit == "u") "unidad" else "peso", amt, unit)
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
