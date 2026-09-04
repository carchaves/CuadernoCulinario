package dev.raflos.cocina.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Íconos de línea dibujados a mano, copiados tal cual de los `<path d="…">` del diseño
 * (`Cocina App.dc.html`). Los SVG del diseño son trazos sin relleno, así que acá se arman
 * como paths con `stroke` y `fill = null` — el color real lo pone `tint` en el `Icon`.
 */
private fun lineIcon(
    name: String,
    viewportSize: Float,
    strokeWidth: Float,
    vararg pathData: String,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = viewportSize,
    viewportHeight = viewportSize,
).apply {
    pathData.forEach { d ->
        addPath(
            pathData = PathParser().parsePathString(d).toNodes(),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
}.build()

// ---- Íconos del Menú (viewBox 52, stroke 2) ----

/** Estante/alacena de despensa. */
val PantryShelfIcon: ImageVector = lineIcon(
    "PantryShelf", 52f, 2f,
    "M15 18 h22 v19 a3 3 0 0 1 -3 3 H18 a3 3 0 0 1 -3 -3 Z",
    "M13 16 h26",
    "M26 12 v4",
    "M15 24 a3 3 0 0 1 -4 0",
    "M37 24 a3 3 0 0 0 4 0",
)

/** El mismo estante, con el tachado que el diseño usa cuando la despensa está desvinculada. */
val PantryShelfOffIcon: ImageVector = lineIcon(
    "PantryShelfOff", 52f, 2.4f,
    "M15 18 h22 v19 a3 3 0 0 1 -3 3 H18 a3 3 0 0 1 -3 -3 Z",
    "M13 16 h26",
    "M26 12 v4",
    "M15 24 a3 3 0 0 1 -4 0",
    "M37 24 a3 3 0 0 0 4 0",
    "M42 10 L10 42",
)

/** Libro abierto (Recetas). */
val BookIcon: ImageVector = lineIcon(
    "Book", 52f, 2f,
    "M26 15 C22 12 15 12 11 13 v25 c4 -1 11 -1 15 2",
    "M26 15 C30 12 37 12 41 13 v25 c-4 -1 -11 -1 -15 2",
    "M26 15 v27",
    "M15 20 h6 M15 25 h6",
    "M31 20 h6 M31 25 h6",
)

/** Carrito con ruedas (Lista de Compra). */
val CartIcon: ImageVector = lineIcon(
    "Cart", 52f, 2f,
    "M11 11 h4 l3 20 h20",
    "M18 16 h24 l-3 12 H20",
    "M22 35.4 a2.6 2.6 0 1 0 0 5.2 a2.6 2.6 0 1 0 0 -5.2 Z",
    "M37 35.4 a2.6 2.6 0 1 0 0 5.2 a2.6 2.6 0 1 0 0 -5.2 Z",
)

// ---- Íconos chicos (viewBox 24, stroke ~1.8) ----

/** Carrito compacto del botón "enviar a la lista" de cada ingrediente en Despensa. */
val CartSmallIcon: ImageVector = lineIcon(
    "CartSmall", 24f, 1.9f,
    "M4 4h3l2.4 11.2a2 2 0 0 0 2 1.6h6.9a2 2 0 0 0 2-1.5L22 8H7",
    "M10.5 18.8 a1.2 1.2 0 1 0 0 2.4 a1.2 1.2 0 1 0 0 -2.4 Z",
    "M18.5 18.8 a1.2 1.2 0 1 0 0 2.4 a1.2 1.2 0 1 0 0 -2.4 Z",
)

/** Estrella del botón "cambiar icono" de la repisa activa. */
val StarIcon: ImageVector = lineIcon(
    "Star", 24f, 1.7f,
    "M12 3l2.3 5.2 5.7.5-4.3 3.8 1.3 5.5L12 15.1 7 18l1.3-5.5L4 8.7l5.7-.5z",
)

/** Lupa del buscador de Despensa. */
val SearchLineIcon: ImageVector = lineIcon(
    "SearchLine", 24f, 1.7f,
    "M21 21l-4.3-4.3M10.5 18a7.5 7.5 0 1 1 0-15 7.5 7.5 0 0 1 0 15Z",
)

/**
 * Set de íconos por repisa: es exactamente el array `ICONS` del diseño, en el mismo orden
 * (la clave `k` se guarda en `PantryPage.iconId`).
 */
data class PantryIconOption(val id: String, val icon: ImageVector)

val PantryIconOptions: List<PantryIconOption> = listOf(
    "frasco" to "M9 3h6v3H9z M8 6h8v13a2 2 0 0 1-2 2h-4a2 2 0 0 1-2-2z M8 11h8",
    "botella" to "M10 3h4v4l2 3v11H8V10l2-3z M8 14h8",
    "bolsa" to "M7 8h10l-1 13H8z M10 8V5a2 2 0 0 1 4 0v3",
    "lata" to "M6 6c0-1.1 2.7-2 6-2s6 .9 6 2-2.7 2-6 2-6-.9-6-2z M6 6v12c0 1.1 2.7 2 6 2s6-.9 6-2V6",
    "fruta" to "M12 21a6 6 0 0 1-5-6c0-4 2-6 5-6s5 2 5 6a6 6 0 0 1-5 6z M12 9V5 M12 5c2.5 0 3.5-1.5 3.5-3",
    "verdura" to "M4 20C4 11 10 5 20 4c1 10-5 16-14 16z M4 20c4-4 7-7 11-9",
    "carne" to "M8 20a6 6 0 0 1 0-12c0-3 3-5 6-4s5 4 4 7-2 5-5 6-5 3-5 3z M10 15h.01",
    "pescado" to "M2 12c3-4 8-6 13-5 3 .6 5 2.5 6 5-1 2.5-3 4.4-6 5-5 1-10-1-13-5z M17 11h.01",
    "pan" to "M4 11c0-3 3.5-5 8-5s8 2 8 5c0 1.5-1.5 2-3 2v5H7v-5c-1.5 0-3-.5-3-2z",
    "leche" to "M9 3h6l3 5v11a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V8z M6 8h12",
    "huevo" to "M12 21c-3.3 0-6-2.4-6-5.7C6 10.5 9 3 12 3s6 7.5 6 12.3c0 3.3-2.7 5.7-6 5.7z",
    "queso" to "M3 12l9-5 9 5v6H3z M3 12h18 M8 15h.01 M13 15h.01",
    "especias" to "M9 8h6v12a1 1 0 0 1-1 1h-4a1 1 0 0 1-1-1z M9 8a3 3 0 0 1 6 0 M11 4h2 M12 11h.01 M12 15h.01",
    "caja" to "M3 8l9-4 9 4v9l-9 4-9-4z M3 8l9 4 9-4 M12 12v9",
    "congelado" to "M12 3v18 M4.5 7.5l15 9 M19.5 7.5l-15 9",
    "bebida" to "M4 8h13v7a5 5 0 0 1-5 5H9a5 5 0 0 1-5-5z M17 10h2a2 2 0 0 1 0 5h-2 M7 4v2 M11 3v3",
).map { (k, d) -> PantryIconOption(k, lineIcon(k, 24f, 1.6f, d)) }

/** Mismo fallback que el diseño: sin `iconId`, el ícono sale del índice de la página. */
fun pantryIconFor(iconId: String?, index: Int): ImageVector =
    PantryIconOptions.firstOrNull { it.id == iconId }?.icon
        ?: PantryIconOptions[(index.coerceAtLeast(0)) % PantryIconOptions.size].icon
