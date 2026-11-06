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
    val initials: String
        get() = name.trim().split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "?" }
}
