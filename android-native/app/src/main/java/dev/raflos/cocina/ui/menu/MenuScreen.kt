package dev.raflos.cocina.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.raflos.cocina.ui.SystemBarsAppearance
import dev.raflos.cocina.ui.theme.BookIcon
import dev.raflos.cocina.ui.theme.CartIcon
import dev.raflos.cocina.ui.theme.MenuColors
import dev.raflos.cocina.ui.theme.PantryShelfIcon
import dev.raflos.cocina.ui.theme.UtensilsIcon

enum class MenuDestination { DESPENSA, MENAJE, RECETAS, COMPRA }

/** Ancho de cada tile respecto del ancho disponible (queda cuadrado y centrado). */
private const val TILE_WIDTH_FRACTION = 0.6f

private data class MenuItem(val destination: MenuDestination, val label: String, val icon: ImageVector)

private val ITEMS = listOf(
    MenuItem(MenuDestination.DESPENSA, "Despensa", PantryShelfIcon),
    MenuItem(MenuDestination.MENAJE, "Menaje", UtensilsIcon),
    MenuItem(MenuDestination.RECETAS, "Recetas", BookIcon),
    MenuItem(MenuDestination.COMPRA, "Compras", CartIcon),
)

@Composable
fun MenuScreen(onGo: (MenuDestination) -> Unit, onSettings: () -> Unit) {
    SystemBarsAppearance(lightBackground = false)
    Box(
        modifier = Modifier.fillMaxSize().background(MenuColors.bg).windowInsetsPadding(WindowInsets.systemBars).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Engranaje en la esquina (y no un tile más, que alargaría la columna).
        IconButton(onClick = onSettings, modifier = Modifier.align(Alignment.TopEnd)) {
            Icon(Icons.Outlined.Settings, contentDescription = "Ajustes", tint = MenuColors.inkSoft)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        ) {
            Text(
                "TU COCINA, EN UN SOLO LUGAR",
                color = MenuColors.inkSoft,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 18.dp),
            )
            // Los tiles van uno debajo del otro; al 60% del ancho siguen siendo cuadrados y
            // los cuatro entran en pantalla (el scroll cubre las pantallas más chicas).
            Column(
                modifier = Modifier.fillMaxWidth(TILE_WIDTH_FRACTION),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                ITEMS.forEach { item -> MenuTile(item.label, item.icon) { onGo(item.destination) } }
            }
        }
    }
}

@Composable
private fun MenuTile(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(MenuColors.tile, RoundedCornerShape(14.dp))
            .border(1.5.dp, MenuColors.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // El diseño dibuja el glifo al 46% del ancho del tile.
        Icon(
            icon,
            contentDescription = label,
            tint = MenuColors.ink,
            modifier = Modifier.fillMaxWidth(0.46f).aspectRatio(1f).padding(bottom = 14.dp),
        )
        Text(
            label.uppercase(),
            color = MenuColors.ink,
            fontSize = 12.sp,
            letterSpacing = 1.5.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
        )
    }
}
