package com.jerrynifemi.oweme

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/** Two arcs: how much of the money owed is in your favour, and how much is not. */
class ArcView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var owedColor: Int = 0xFF00E5C0.toInt()
        set(value) { field = value; invalidate() }
    var oweColor: Int = 0xFFFF6B9A.toInt()
        set(value) { field = value; invalidate() }
    var trackColor: Int = 0x22FFFFFF
        set(value) { field = value; invalidate() }

    private var owed = 0f
    private var owe = 0f
    private var progress = 0f
    private val rect = RectF()
    private var stroke = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    fun set(owedMinor: Long, oweMinor: Long, animate: Boolean = true) {
        owed = owedMinor.toFloat()
        owe = oweMinor.toFloat()
        if (!animate) {
            progress = 1f
            invalidate()
            return
        }
        progress = 0f
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 900L
            interpolator = SpringInterpolator(0.78f, 1.45f)
            addUpdateListener {
                progress = (it.animatedValue as Float).coerceIn(0f, 1.08f)
                invalidate()
            }
        }.start()
    }

    override fun onDraw(canvas: Canvas) {
        val size = min(width, height).toFloat()
        if (size <= 0f) return
        stroke = size * 0.115f
        val inset = stroke / 2f + size * 0.04f
        rect.set(inset, inset, width - inset, height - inset)
        paint.strokeWidth = stroke

        paint.color = trackColor
        canvas.drawArc(rect, 0f, 360f, false, paint)

        val total = owed + owe
        if (total <= 0f) return

        val owedSweep = 360f * (owed / total) * progress
        val oweSweep = 360f * (owe / total) * progress

        paint.color = owedColor
        canvas.drawArc(rect, -90f, owedSweep, false, paint)
        paint.color = oweColor
        canvas.drawArc(rect, -90f + owedSweep, oweSweep, false, paint)
    }
}
