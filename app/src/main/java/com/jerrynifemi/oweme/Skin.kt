package com.jerrynifemi.oweme

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

/**
 * Recolours an inflated screen to match the chosen look. Screens are inflated with the
 * dark palette, so light mode works by remapping those exact values rather than by
 * duplicating every layout.
 */
object Skin {

    private const val DARK_PRIMARY = -0x1 // 0xFFFFFFFF
    private const val DARK_MUTED = 0xFFC7CCE6.toInt()
    private const val LIGHT_PRIMARY = 0xFF141026.toInt()
    private const val LIGHT_MUTED = 0xFF5B6280.toInt()

    fun accent(settings: Settings): Int = Settings.ACCENTS[settings.accentIndex % Settings.ACCENTS.size]

    fun apply(root: View, settings: Settings) {
        val light = settings.isLight
        val map: Map<Int, Int> = if (light)
            mapOf(DARK_PRIMARY to LIGHT_PRIMARY, DARK_MUTED to LIGHT_MUTED)
        else
            mapOf(LIGHT_PRIMARY to DARK_PRIMARY, LIGHT_MUTED to DARK_MUTED)

        val tint = if (light) withAlpha(Color.WHITE, 168) else withAlpha(Color.WHITE, 26)
        val border = if (light) withAlpha(Color.WHITE, 220) else withAlpha(Color.WHITE, 61)
        val glassScale = settings.glass / 22f

        walk(root) { view ->
            when (view) {
                is GlassPanel -> {
                    view.tintColor = withAlpha(tint, (Color.alpha(tint) * glassScale).toInt().coerceIn(8, 235))
                    view.borderColor = border
                    view.invalidate()
                }
                is TextView -> {
                    map[view.currentTextColor]?.let { view.setTextColor(it) }
                }
            }
        }
    }

    private fun walk(view: View, action: (View) -> Unit) {
        action(view)
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) walk(view.getChildAt(i), action)
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
}
