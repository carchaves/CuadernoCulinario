package dev.raflos.cocina.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Icon
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
import dev.raflos.cocina.ui.theme.MenuColors

enum class MenuDestination { DESPENSA, RECETAS, COMPRA }

private data class MenuItem(val destination: MenuDestination, val label: String, val icon: ImageVector)

private val ITEMS = listOf(
    MenuItem(MenuDestination.DESPENSA, "Despensa", Icons.Outlined.Inventory2),
    MenuItem(MenuDestination.RECETAS, "Recetas", Icons.Outlined.Book),
    MenuItem(MenuDestination.COMPRA, "Lista de Compra", Icons.Outlined.ShoppingCart),
)

@Composable
fun MenuScreen(onGo: (MenuDestination) -> Unit) {
    SystemBarsAppearance(lightBackground = false)
    Box(
        modifier = Modifier.fillMaxSize().background(MenuColors.bg).windowInsetsPadding(WindowInsets.systemBars).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "TU COCINA, EN UN SOLO LUGAR",
                color = MenuColors.inkSoft,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 18.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                ITEMS.forEach { item -> MenuTile(item.label, item.icon) { onGo(item.destination) } }
            }
        }
    }
}

@Composable
private fun RowScope.MenuTile(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .background(MenuColors.tile, RoundedCornerShape(14.dp))
            .border(1.5.dp, MenuColors.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = label, tint = MenuColors.ink, modifier = Modifier.padding(bottom = 14.dp))
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
