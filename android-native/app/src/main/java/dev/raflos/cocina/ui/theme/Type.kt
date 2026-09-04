package dev.raflos.cocina.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import dev.raflos.cocina.R

/**
 * Las tipografías reales del diseño (`Cocina App.dc.html`), como archivos estáticos en
 * `res/font/` — nada de Google Fonts descargables en runtime.
 *
 * Uso 1:1 con el diseño:
 *  - Fraunces          → títulos de Despensa.
 *  - Inter             → cuerpo de Despensa.
 *  - Instrument Serif  → títulos/números de tarjeta de Recetas (itálica para los números).
 *  - DM Sans           → cuerpo de Recetas.
 *  - JetBrains Mono    → todo valor numérico, en cualquier pantalla.
 */

val FraunceFamily = FontFamily(
    Font(R.font.fraunces_400, FontWeight.Normal),
    Font(R.font.fraunces_500, FontWeight.Medium),
    Font(R.font.fraunces_600, FontWeight.SemiBold),
    Font(R.font.fraunces_700, FontWeight.Bold),
)

val InterFamily = FontFamily(
    Font(R.font.inter_400, FontWeight.Normal),
    Font(R.font.inter_500, FontWeight.Medium),
    Font(R.font.inter_600, FontWeight.SemiBold),
)

val DMSansFamily = FontFamily(
    Font(R.font.dm_sans_400, FontWeight.Normal),
    Font(R.font.dm_sans_500, FontWeight.Medium),
    Font(R.font.dm_sans_600, FontWeight.SemiBold),
    Font(R.font.dm_sans_700, FontWeight.Bold),
)

/** Registra ambos estilos: pedir `fontStyle = FontStyle.Italic` alcanza para la itálica. */
val InstrumentSerifFamily = FontFamily(
    Font(R.font.instrument_serif_400, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.instrument_serif_400_italic, FontWeight.Normal, FontStyle.Italic),
)

val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono_500, FontWeight.Medium),
    Font(R.font.jetbrains_mono_600, FontWeight.SemiBold),
)

/** Alias histórico usado por Lista de Compra y Ajustes (pantallas fuera de este rediseño). */
val SerifFamily = FraunceFamily

/** Alias histórico para valores numéricos. */
val MonoFamily = JetBrainsMonoFamily
