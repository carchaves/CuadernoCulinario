package dev.raflos.cocina.data

import dev.raflos.cocina.data.model.Ingredient
import java.util.UUID
import kotlin.math.max
import kotlin.math.round

/**
 * Port of app/src/core/logic.ts — kept behavior-identical (including its known false
 * positives in fuzzy ingredient-name matching, e.g. "tortillas de harina" matching "Harina").
 */

fun uid(): String = UUID.randomUUID().toString()

fun stepFor(unit: String): Double = when (unit) {
    "kg", "L" -> 0.1
    "g", "ml" -> 50.0
    else -> 1.0
}

fun roundFor(v: Double, unit: String): Double {
    val r = if (unit == "kg" || unit == "L") round(v * 100) / 100 else round(v)
    return max(0.0, r)
}

fun fmt(n: Double?, unit: String): String {
    if (n == null || n.isNaN()) return "0"
    val dec = if (unit == "kg" || unit == "L") 2 else 0
    var s = String.format(java.util.Locale.ROOT, "%.${dec}f", n)
    if (dec > 0) s = s.trimEnd('0').trimEnd('.')
    return s
}

private fun fmtNum(n: Double): String {
    val rounded = round(n * 100) / 100
    if (rounded == round(rounded)) return rounded.toLong().toString()
    return String.format(java.util.Locale.ROOT, "%.2f", rounded).trimEnd('0').trimEnd('.')
}

fun cleanIngName(name: String): String {
    var n = name.trim()
    val re = Regex("^u\\s+de\\s+", RegexOption.IGNORE_CASE)
    while (re.containsMatchIn(n)) n = re.replaceFirst(n, "").trim()
    return n
}

data class SplitIngredient(val name: String, val num: String, val unit: String)

private val SPLIT_RE = Regex(
    "^([\\d][\\d.,/–-]*)\\s*(kg|g|ml|l|cdas?|tazas?|docenas?|dientes?|claras?|u)?\\b\\s*(?:de\\s+)?(.+)$",
    RegexOption.IGNORE_CASE
)

fun splitIngredient(str: String): SplitIngredient {
    val m = SPLIT_RE.find(str)
    return if (m != null) {
        SplitIngredient(
            name = cleanIngName(m.groupValues[3]),
            num = m.groupValues[1].trim(),
            unit = m.groupValues[2].trim(),
        )
    } else {
        SplitIngredient(name = cleanIngName(str), num = "", unit = "")
    }
}

fun buildIngString(name: String, amount: Double, unit: String): String {
    if (amount == 0.0) return name
    val amt = fmt(amount, unit)
    return if (unit.isNotEmpty()) "$amt $unit de $name" else "$amt de $name"
}

fun findPantryUnit(pantryFlat: List<Ingredient>, name: String): String {
    val low = name.lowercase()
    val match = pantryFlat.firstOrNull { pi ->
        val pn = pi.name.lowercase()
        low.contains(pn) || pn.contains(low)
    }
    return match?.unit ?: "u"
}

data class ParsedIngredient(val id: String, val name: String, val amount: Double, val unit: String)

fun parseIngToObj(str: String, pantryFlat: List<Ingredient>): ParsedIngredient {
    val p = splitIngredient(str)
    val unit = findPantryUnit(pantryFlat, p.name).ifEmpty { p.unit.ifEmpty { "u" } }
    val amount = p.num.replace(',', '.').toDoubleOrNull() ?: 0.0
    return ParsedIngredient(id = uid(), name = p.name, amount = amount, unit = unit)
}

fun parseMinutes(txt: String?): Double {
    if (txt.isNullOrEmpty()) return Double.POSITIVE_INFINITY
    var mins = 0.0
    Regex("(\\d+(?:[.,]\\d+)?)\\s*h", RegexOption.IGNORE_CASE).findAll(txt).forEach {
        mins += (it.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 0.0) * 60
    }
    Regex("(\\d+)\\s*min", RegexOption.IGNORE_CASE).findAll(txt).forEach {
        mins += it.groupValues[1].toDoubleOrNull() ?: 0.0
    }
    return if (mins == 0.0) Double.POSITIVE_INFINITY else mins
}

fun gramsFor(u: String): Double? = when (u) {
    "kg", "L" -> 1000.0
    "g", "ml" -> 1.0
    else -> null
}

fun hasAllIngredients(ingredientes: List<String>, pantryNames: List<String>): Boolean {
    if (ingredientes.isEmpty()) return false
    return ingredientes.all { ig ->
        val low = ig.lowercase()
        pantryNames.any { pn -> low.contains(pn) || pn.contains(low) }
    }
}

sealed class Availability { data object En : Availability(); data object Pocas : Availability(); data object Sin : Availability() }

data class AvailabilityIngredient(
    val name: String,
    val num: String,
    val numDisplay: String,
    val unitDisplay: String,
    val available: Availability,
)

data class AvailabilityResult(
    val ingEn: List<AvailabilityIngredient>,
    val ingPocas: List<AvailabilityIngredient>,
    val ingSin: List<AvailabilityIngredient>,
)

/** Groups a recipe's ingredient strings by pantry availability, scaling quantities by servings/4. */
fun computeIngredientAvailability(
    ingredientes: List<String>,
    pantryFlat: List<Ingredient>,
    servings: Int,
): AvailabilityResult {
    val ratio = servings / 4.0
    fun findMatch(name: String): Ingredient? {
        val low = name.lowercase()
        return pantryFlat.firstOrNull { pi ->
            val pn = pi.name.lowercase()
            low.contains(pn) || pn.contains(low)
        }
    }

    val ingEn = mutableListOf<AvailabilityIngredient>()
    val ingPocas = mutableListOf<AvailabilityIngredient>()
    val ingSin = mutableListOf<AvailabilityIngredient>()

    ingredientes.map(::splitIngredient).forEach { ig ->
        val match0 = findMatch(ig.name)
        val scaledNum = if (ig.num.isNotEmpty()) {
            fmtNum((ig.num.replace(',', '.').toDoubleOrNull() ?: 0.0) * ratio)
        } else ig.num
        val unit = if (scaledNum.isNotEmpty()) ig.unit.ifEmpty { match0?.unit ?: "u" } else ""

        val match = findMatch(ig.name)
        if (match == null) {
            ingSin.add(AvailabilityIngredient(ig.name, scaledNum, scaledNum, if (scaledNum.isNotEmpty()) unit else "", Availability.Sin))
            return@forEach
        }
        val gU = gramsFor(unit)
        val gP = gramsFor(match.unit)
        val needed = scaledNum.toDoubleOrNull()
        when {
            needed != null && gU != null && gP != null -> {
                val have = (match.amount * gP) / gU
                val label = "${fmtNum(have)}/${fmtNum(needed)}"
                val bucket = if (have >= needed) ingEn else ingPocas
                bucket.add(AvailabilityIngredient(ig.name, scaledNum, label, match.unit, if (have >= needed) Availability.En else Availability.Pocas))
            }
            needed != null && (unit == "u" || gU == null) -> {
                val have = match.amount
                val label = "${fmtNum(have)}/${fmtNum(needed)}"
                val bucket = if (have >= needed) ingEn else ingPocas
                bucket.add(AvailabilityIngredient(ig.name, scaledNum, label, match.unit, if (have >= needed) Availability.En else Availability.Pocas))
            }
            else -> {
                ingEn.add(AvailabilityIngredient(ig.name, scaledNum, scaledNum, if (scaledNum.isNotEmpty()) unit else "", Availability.En))
            }
        }
    }

    return AvailabilityResult(ingEn, ingPocas, ingSin)
}

fun stepId(view: String, recId: String, section: String, i: Int): String = "$view.$recId.$section.$i"
