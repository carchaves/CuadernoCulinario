package dev.raflos.cocina.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Ajusta el color de los íconos de la barra de estado y de navegación (hora, batería, botones
 * virtuales) según el fondo de la pantalla actual: íconos oscuros sobre fondo claro, íconos
 * claros sobre fondo oscuro. Llamar una vez al principio de cada pantalla con su propio fondo.
 */
@Composable
fun SystemBarsAppearance(lightBackground: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = (view.context as Activity).window
    SideEffect {
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = lightBackground
        controller.isAppearanceLightNavigationBars = lightBackground
    }
}
