package com.jerrynifemi.oweme

/** One line in the ledger. [direction] is +1 when they owe me, -1 when I owe them. */
data class Entry(
    val id: String,
    val person: String,
    val amountMinor: Long,
    val direction: Int,
    val note: String,
    val timestamp: Long
) {
    val signedMinor: Long get() = amountMinor * direction
}

/** Everything one person owes, rolled up. Positive balance means they owe me. */
data class PersonBalance(
    val name: String,
    val balanceMinor: Long,
    val entries: List<Entry>
) {

    /** "Jade And Juke", "Chidi & Ada", "Team A, B" are groups, not people. */
    val isGroup: Boolean
        get() {
            val lower = name.lowercase()
            return lower.contains(" and ") || lower.contains("&") ||
                lower.contains("+") || lower.contains(",") || lower.contains(" x ")
        }

    /**
     * Two letters that mean something. Connector words are skipped, so
     * "Jude And Nike" is JN rather than JA.
     */
    val initials: String
        get() {
            val words = name.trim()
                .split(" ")
                .map { it.trim() }
                .filter { it.isNotEmpty() && it.lowercase() !in CONNECTORS }
            val letters = words.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }
            return if (letters.isEmpty()) "?" else letters.joinToString("")
        }

    private companion object {
        val CONNECTORS = setOf("and", "&", "+", "x", "with", "the")
    }
}

/** A different glass tint per name, so the list reads as people rather than rows. */
object Accents {
    private val colors = intArrayOf(
        0xFF7C4DFF.toInt(),
        0xFF00B894.toInt(),
        0xFFFF6B9A.toInt(),
        0xFF4D9BFF.toInt(),
        0xFFFFB020.toInt(),
        0xFFB388FF.toInt()
    )

    fun of(name: String): Int {
        val hash = name.fold(7) { acc, ch -> acc * 31 + ch.code }
        return colors[Math.abs(hash) % colors.size]
    }
}
