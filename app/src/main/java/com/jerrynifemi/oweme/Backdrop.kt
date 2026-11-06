package com.jerrynifemi.oweme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri

/** Loads the optional photo backdrop without blowing up memory, and builds palettes. */
object Backdrop {

    fun load(context: Context, uriString: String?, maxSize: Int): Bitmap? {
        if (uriString.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(uriString)
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0) return null

            var sample = 1
            while (bounds.outWidth / sample > maxSize * 2 || bounds.outHeight / sample > maxSize * 2) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        } catch (e: Exception) {
            null
        }
    }

    fun mix(from: Int, to: Int, amount: Float): Int {
        val r = (Color.red(from) + (Color.red(to) - Color.red(from)) * amount).toInt()
        val g = (Color.green(from) + (Color.green(to) - Color.green(from)) * amount).toInt()
        val b = (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * amount).toInt()
        return Color.rgb(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }

    fun withAlpha(color: Int, alpha: Float): Int =
        Color.argb((alpha * 255).toInt().coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

    /** Four drifting blob colours derived from the chosen accent. */
    fun blobs(accent: Int, light: Boolean): IntArray {
        val soft: Int = if (light) Color.WHITE else 0xFF101528.toInt()
        val a = if (light) mix(accent, Color.WHITE, 0.35f) else accent
        return intArrayOf(
            a,
            mix(a, 0xFF00E5C0.toInt(), 0.55f),
            mix(a, 0xFFFF6B9A.toInt(), 0.5f),
            mix(a, soft, 0.45f)
        )
    }
}
