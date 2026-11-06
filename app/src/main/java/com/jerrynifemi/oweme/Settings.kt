package com.jerrynifemi.oweme

import android.content.Context

/**
 * How the app looks and behaves. Style (glass / solid / flex) and tone (dark /
 * light) are independent, so solid light and glassy dark both exist.
 */
class Settings(context: Context) {

    private val p =
        context.applicationContext.getSharedPreferences("owe_me_settings", Context.MODE_PRIVATE)

    val revision: Int get() = p.getInt("rev", 0)

    fun touch() {
        p.edit().putInt("rev", revision + 1).commit()
    }

    var soundOn: Boolean
        get() = p.getBoolean("sound", true)
        set(v) = p.edit().putBoolean("sound", v).apply()

    var currencyCode: String
        get() = p.getString("currency", "NGN") ?: "NGN"
        set(v) {
            p.edit().putString("currency", v).apply()
        }

    val currencySymbol: String
        get() = CURRENCIES.firstOrNull { it.first == currencyCode }?.second ?: "₦"

    /** glass | solid | flex */
    var style: String
        get() = p.getString("style", null) ?: migratedStyle()
        set(v) = p.edit().putString("style", v).apply()

    /** dark | light */
    var tone: String
        get() = p.getString("tone", null) ?: migratedTone()
        set(v) = p.edit().putString("tone", v).apply()

    var accentIndex: Int
        get() = p.getInt("accent", 0).coerceIn(0, ACCENTS.size - 1)
        set(v) = p.edit().putInt("accent", v.coerceIn(0, ACCENTS.size - 1)).apply()

    var pulse: Boolean
        get() = p.getBoolean("pulse", true)
        set(v) = p.edit().putBoolean("pulse", v).apply()

    var blur: Int
        get() = p.getInt("blur", 28)
        set(v) = p.edit().putInt("blur", v).apply()

    var glassOpacity: Int
        get() = p.getInt("glassOpacity", 72)
        set(v) = p.edit().putInt("glassOpacity", v).apply()

    var backdrop: String?
        get() = p.getString("backdrop", null)
        set(v) = p.edit().putString("backdrop", v).apply()

    val isLight: Boolean get() = tone == "light"
    val isGlass: Boolean get() = style != "solid"

    /** Older builds stored a single "theme" value; carry those choices forward. */
    private fun migratedStyle(): String =
        if (p.getString("theme", "dark") == "flex") "flex" else "glass"

    private fun migratedTone(): String =
        if (p.getString("theme", "dark") == "light") "light" else "dark"

    fun resetLook() {
        p.edit()
            .remove("style").remove("tone").remove("accent")
            .remove("pulse").remove("blur").remove("glassOpacity").remove("backdrop")
            .apply()
        touch()
    }

    companion object {
        val CURRENCIES = listOf(
            "NGN" to "₦", "USD" to "$", "EUR" to "€", "GBP" to "£",
            "GHS" to "₵", "KES" to "KSh", "ZAR" to "R", "INR" to "₹"
        )

        /** accent, name */
        val ACCENTS = listOf(
            0xFF7C4DFF.toInt() to "Violet",
            0xFF00C2A8.toInt() to "Teal",
            0xFFFF5C8A.toInt() to "Rose",
            0xFF3D8BFF.toInt() to "Ocean",
            0xFFFFA23A.toInt() to "Amber",
            0xFF9B5CFF.toInt() to "Grape"
        )
    }
}
