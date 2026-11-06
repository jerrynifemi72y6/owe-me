package com.jerrynifemi.oweme

import java.text.NumberFormat
import java.util.Locale

/** Naira first, because that is where this app lives. Amounts are stored in kobo. */
object Money {

    private val whole: NumberFormat = NumberFormat.getIntegerInstance(Locale.US)

    fun full(minor: Long, withSign: Boolean = false): String {
        val negative = minor < 0
        val value = Math.abs(minor)
        val naira = value / 100
        val kobo = value % 100
        val body = if (kobo == 0L) "₦" + whole.format(naira)
        else "₦" + whole.format(naira) + "." + String.format(Locale.US, "%02d", kobo)
        return when {
            withSign && negative -> "-" + body
            withSign && minor > 0 -> "+" + body
            negative -> "-" + body
            else -> body
        }
    }

    fun parse(text: String): Long? {
        val cleaned = text.replace(",", "").replace("₦", "").trim()
        if (cleaned.isEmpty()) return null
        val value = cleaned.toDoubleOrNull() ?: return null
        if (value <= 0 || value > 1_000_000_000) return null
        return Math.round(value * 100)
    }
}
