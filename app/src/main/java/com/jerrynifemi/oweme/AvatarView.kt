package com.jerrynifemi.oweme

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

/**
 * Glass avatar. Individuals get initials, groups get overlapping figures, so a
 * group never reads as a person whose name happens to start with "And".
 */
class AvatarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var initials: String = "?"
        set(value) {
            field = value
            invalidate()
        }

    var isGroup: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    var accent: Int = Color.parseColor("#7C4DFF")
        set(value) {
            field = value
            invalidate()
        }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sheen = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.1f)
        color = Color.argb(90, 255, 255, 255)
    }
    private val figure = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.6f)
        color = Color.argb(230, 255, 255, 255)
    }
    private val head = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        color = Color.WHITE
    }

    private var shaderSize = 0f

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val r = minOf(w, h) / 2f
        val cx = w / 2f
        val cy = h / 2f

        if (shaderSize != r) {
            shaderSize = r
            fill.shader = RadialGradient(
                cx - r * 0.3f, cy - r * 0.35f, r * 1.4f,
                intArrayOf(withAlpha(accent, 150), withAlpha(accent, 60), Color.argb(10, 255, 255, 255)),
                floatArrayOf(0f, 0.6f, 1f),
                Shader.TileMode.CLAMP
            )
            sheen.shader = LinearGradient(
                0f, 0f, 0f, h,
                Color.argb(70, 255, 255, 255), Color.argb(0, 255, 255, 255),
                Shader.TileMode.CLAMP
            )
        }

        canvas.drawCircle(cx, cy, r, fill)
        canvas.drawCircle(cx, cy, r, sheen)
        canvas.drawCircle(cx, cy, r - dp(0.5f), rim)

        if (isGroup) {
            val cr = r * 0.40f
            val offset = r * 0.26f
            head.color = Color.argb(60, 255, 255, 255)
            canvas.drawCircle(cx - offset, cy - r * 0.08f, cr, head)
            head.color = Color.argb(30, 255, 255, 255)
            canvas.drawCircle(cx + offset, cy + r * 0.10f, cr, head)
            figure.color = Color.argb(230, 255, 255, 255)
            canvas.drawCircle(cx - offset, cy - r * 0.08f, cr, figure)
            figure.color = Color.argb(160, 255, 255, 255)
            canvas.drawCircle(cx + offset, cy + r * 0.10f, cr, figure)
        } else {
            text.textSize = h * 0.36f
            val baseY = cy - (text.descent() + text.ascent()) / 2f
            canvas.drawText(initials, cx, baseY, text)
        }
    }

    private fun dp(value: Float) = value * resources.displayMetrics.density

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
}
