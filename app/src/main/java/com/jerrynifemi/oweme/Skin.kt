package com.jerrynifemi.oweme

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

/**
 * A resolved palette. Built once from settings, then applied to every inflated
 * screen so dark / light and glass / solid all share one code path.
 */
data class Skin(
    val bgTop: Int,
    val bgBottom: Int,
    val ink: Int,
    val muted: Int,
    val accent: Int,
    val accentDeep: Int,
    val good: Int,
    val bad: Int,
    val cardFill: Int,
    val cardBorder: Int,
    val frosted: Boolean,
    val pulse: Boolean,
    val blur: Int,
    val photo: String?
) {

    val accentOnCard: Int get() = if (isLightBg) accentDeep else accent
    val isLightBg: Boolean get() = Color.luminance(bgBottom) > 0.5f

    fun applyTo(root: View) = tint(root)

    private fun tint(v: View) {
        if (v is TextView) {
            when (v.currentTextColor) {
                DARK_PRIMARY -> v.setTextColor(ink)
                DARK_MUTED -> v.setTextColor(muted)
            }
        }
        if (v is GlassPanel) {
            v.frosted = frosted
            v.tintColor = cardFill
            v.borderColor = cardBorder
        }
        if (v is ViewGroup) for (i in 0 until v.childCount) tint(v.getChildAt(i))
    }

    companion object {
        const val DARK_PRIMARY = -0x1
        const val DARK_MUTED = 0xFFC7CCE6.toInt()

        fun of(s: Settings): Skin {
            val accent = Settings.ACCENTS[s.accentIndex].first
            val glass = s.isGlass
            val opacity = (s.glassOpacity / 100f).coerceIn(0.25f, 1f)
            return if (s.isLight) Skin(
                bgTop = 0xFFF2F4FF.toInt(),
                bgBottom = 0xFFFFFFFF.toInt(),
                ink = 0xFF11102A.toInt(),
                muted = 0xFF5A607E.toInt(),
                accent = accent,
                accentDeep = darken(accent, 0.28f),
                good = 0xFF00A98C.toInt(),
                bad = 0xFFDC3D74.toInt(),
                cardFill = if (glass) whiteGlass(opacity) else 0xFFFFFFFF.toInt(),
                cardBorder = if (glass) 0xFFFFFFFF.toInt() else 0x22000000,
                frosted = glass,
                pulse = glass && s.pulse,
                blur = if (glass) s.blur else 0,
                photo = if (glass) s.backdrop else null
            ) else Skin(
                bgTop = 0xFF0B1026.toInt(),
                bgBottom = 0xFF05060E.toInt(),
                ink = -0x1,
                muted = 0xFFC7CCE6.toInt(),
                accent = accent,
                accentDeep = lighten(accent, 0.22f),
                good = 0xFF00E5C0.toInt(),
                bad = 0xFFFF6B9A.toInt(),
                cardFill = if (glass) darkGlass(opacity) else 0xFF151A2E.toInt(),
                cardBorder = if (glass) 0x3DFFFFFF else 0x2EFFFFFF,
                frosted = glass,
                pulse = glass && s.pulse,
                blur = if (glass) s.blur else 0,
                photo = if (glass) s.backdrop else null
            )
        }

        private fun darkGlass(opacity: Float): Int =
            Color.argb((14 + 58 * opacity).toInt(), 255, 255, 255)

        private fun whiteGlass(opacity: Float): Int =
            Color.argb((120 + 110 * opacity).toInt(), 255, 255, 255)

        fun darken(color: Int, amount: Float): Int = Color.rgb(
            (Color.red(color) * (1 - amount)).toInt(),
            (Color.green(color) * (1 - amount)).toInt(),
            (Color.blue(color) * (1 - amount)).toInt()
        )

        fun lighten(color: Int, amount: Float): Int = Color.rgb(
            (Color.red(color) + (255 - Color.red(color)) * amount).toInt(),
            (Color.green(color) + (255 - Color.green(color)) * amount).toInt(),
            (Color.blue(color) + (255 - Color.blue(color)) * amount).toInt()
        )
    }
}
