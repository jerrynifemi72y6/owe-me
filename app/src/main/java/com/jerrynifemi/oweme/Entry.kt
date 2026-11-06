package com.jerrynifemi.oweme

import android.graphics.Color

/**
 * One line in the ledger. [direction] is +1 when they owe me, -1 when I owe them.
 * [paidMinor] tracks partial payments so a ₦5,000 debt can be ₦2,000 settled.
 */
data class Entry(
    val id: String,
    val person: String,
    val amountMinor: Long,
    val direction: Int,
    val note: String,
    val timestamp: Long,
    val paidMinor: Long = 0L
) {
    val remainingMinor: Long get() = (amountMinor - paidMinor).coerceAtLeast(0L)
    val signedMinor: Long get() = remainingMinor * direction
    val isPartPaid: Boolean get() = paidMinor > 0L && remainingMinor > 0L
    val isPaidOff: Boolean get() = remainingMinor == 0L
}

/** Everything one person owes, rolled up. Positive balance means they owe me. */
data class PersonBalance(
    val name: String,
    val balanceMinor: Long,
    val entries: List<Entry>
) {
    /** "Jade And Juke" is a group, "Jude Nike" is a person. */
    val isGroup: Boolean
        get() {
            val lower = name.lowercase()
            return lower.contains(" and ") || lower.contains("&") ||
                lower.contains("+") || lower.contains(",") || lower.contains(" x ")
        }

    val initials: String
        get() {
            val words = name.split(" ", ",")
                .map { it.trim() }
                .filter { it.isNotEmpty() && it.lowercase() !in CONNECTORS }
            val letters = words.filter { it.first().isLetter() }
                .take(2)
                .joinToString("") { it.first().uppercase() }
            return letters.ifEmpty { "?" }
        }

    private companion object {
        val CONNECTORS = setOf("and", "&", "+", "x", "with")
    }
}

/** A stable pastel per name, so the same person always looks the same. */
object Accents {
    private val palette = intArrayOf(
        Color.parseColor("#7C4DFF"),
        Color.parseColor("#00B894"),
        Color.parseColor("#FF6B9A"),
        Color.parseColor("#4D9BFF"),
        Color.parseColor("#FFB020"),
        Color.parseColor("#B388FF")
    )

    fun of(name: String): Int = palette[(name.lowercase().hashCode() and 0x7fffffff) % palette.size]
}
