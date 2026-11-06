package com.jerrynifemi.oweme

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

/** Seven glass bars, one per day, growing in with the same spring as the rest of the app. */
class WeekChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var values = FloatArray(7)
    private var labels = arrayOf("", "", "", "", "", "", "")
    private var progress = 0f

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(20, 255, 255, 255) }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.parseColor("#C7CCE6")
    }
    private val rect = RectF()
    private var shader: Shader? = null

    fun setData(sums: FloatArray, days: Array<String>, animate: Boolean = true) {
        values = sums
        labels = days
        if (animate) {
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 820
                interpolator = SpringInterpolator(0.72f, 1.5f)
                addUpdateListener {
                    progress = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            progress = 1f
            invalidate()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        shader = LinearGradient(
            0f, h.toFloat(), 0f, 0f,
            intArrayOf(Color.parseColor("#7C4DFF"), Color.parseColor("#00E5C0")),
            null,
            Shader.TileMode.CLAMP
        )
        labelPaint.textSize = dp(10f)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val labelSpace = dp(18f)
        val usableH = h - labelSpace
        val slot = w / values.size
        val barW = slot * 0.54f
        val radius = barW / 2f
        val max = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
        val eased = progress.coerceIn(0f, 1.12f)

        values.forEachIndexed { index, value ->
            val cx = slot * index + slot / 2f
            val left = cx - barW / 2f

            // empty track, so a quiet day still reads as a day
            rect.set(left, usableH - dp(3f), left + barW, usableH)
            canvas.drawRoundRect(rect, radius, radius, trackPaint)

            if (value > 0f) {
                val barH = (value / max) * (usableH - dp(6f)) * eased
                val top = usableH - barH
                rect.set(left, top, left + barW, usableH)
                barPaint.shader = shader
                canvas.drawRoundRect(rect, radius, radius, barPaint)
                barPaint.shader = null
            }

            val label = labels.getOrElse(index) { "" }
            if (label.isNotEmpty()) {
                val baseY = h - dp(4f)
                canvas.drawText(label, cx, baseY, labelPaint)
            }
        }
    }

    private fun dp(value: Float) = value * resources.displayMetrics.density
}
