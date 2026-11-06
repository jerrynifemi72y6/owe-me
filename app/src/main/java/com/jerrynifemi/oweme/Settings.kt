package com.jerrynifemi.oweme

import android.content.Context

/** Everything the settings screen writes, and every screen reads back. */
class Settings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("owe_me_settings", Context.MODE_PRIVATE)

    var soundOn: Boolean
        get() = prefs.getBoolean("sound", true)
        set(value) = prefs.edit().putBoolean("sound", value).apply()

    /** Just the symbol: ₦, $, €, and so on. */
    var currency: String
        get() = prefs.getString("currency", "₦") ?: "₦"
        set(value) = prefs.edit().putString("currency", value).apply()

    var currencyCode: String
        get() = prefs.getString("currency_code", "NGN") ?: "NGN"
        set(value) = prefs.edit().putString("currency_code", value).apply()

    /** dark, light or flex. */
    var theme: String
        get() = prefs.getString("theme", "dark") ?: "dark"
        set(value) = prefs.edit().putString("theme", value).apply()

    val isLight: Boolean get() = theme == "light"

    var accentIndex: Int
        get() = prefs.getInt("accent", 0)
        set(value) = prefs.edit().putInt("accent", value).apply()

    var pulse: Boolean
        get() = prefs.getBoolean("pulse", true)
        set(value) = prefs.edit().putBoolean("pulse", value).apply()

    /** Blur radius in px applied to the drifting background, 0 disables it. */
    var blur: Int
        get() = prefs.getInt("blur", 48)
        set(value) = prefs.edit().putInt("blur", value).apply()

    /** Glass fill opacity, 10 to 90 percent. */
    var glass: Int
        get() = prefs.getInt("glass", 22)
        set(value) = prefs.edit().putInt("glass", value).apply()

    var backdropUri: String?
        get() = prefs.getString("backdrop", null)
        set(value) = prefs.edit().putString("backdrop", value).apply()

    /** Bumped whenever something visual changes, so screens know to rebuild. */
    var revision: Int
        get() = prefs.getInt("revision", 0)
        set(value) = prefs.edit().putInt("revision", value).apply()

    fun touch() {
        revision = revision + 1
    }

    fun resetLook() {
        theme = "dark"
        accentIndex = 0
        pulse = true
        blur = 48
        glass = 22
        backdropUri = null
        touch()
    }

    companion object {
        val CURRENCIES = listOf(
            "NGN" to "₦",
            "USD" to "$",
            "EUR" to "€",
            "GBP" to "£",
            "GHS" to "₵",
            "KES" to "KSh",
            "ZAR" to "R",
            "INR" to "₹"
        )

        val ACCENTS = listOf(
            0xFF7C4DFF.toInt(),
            0xFF00B3A4.toInt(),
            0xFFFF6B9A.toInt(),
            0xFF4D9BFF.toInt(),
            0xFFFFB020.toInt(),
            0xFFB388FF.toInt()
        )
    }
}
