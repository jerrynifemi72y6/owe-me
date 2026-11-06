package com.jerrynifemi.oweme

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.Choreographer
import android.view.View

/**
 * Slow-drifting colour blobs behind everything. This is what the glass panels are
 * refracting, so the whole screen quietly breathes instead of sitting flat.
 * On Android 12+ the blobs get a real GPU blur pass; older devices get the
 * gradients as-is, which still reads soft.
 */
class LiquidBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val blobPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var baseShader: Shader? = null
    private val blobShaders = arrayOfNulls<RadialGradient>(BLOB_COLORS.size)
    private var radius = 0f
    private var time = 0.0

    init {
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            setRenderEffect(
                android.graphics.RenderEffect.createBlurEffect(58f, 58f, Shader.TileMode.CLAMP)
            )
        }
    }

    private val ticker = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            time = frameTimeNanos / 1_000_000_000.0
            invalidate()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Choreographer.getInstance().postFrameCallback(ticker)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Choreographer.getInstance().removeFrameCallback(ticker)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        baseShader = LinearGradient(
            0f, 0f, w * 0.35f, h.toFloat(),
            intArrayOf(BASE_TOP, BASE_BOTTOM),
            null, Shader.TileMode.CLAMP
        )
        radius = (Math.min(w, h) * 0.62f).coerceAtLeast(160f)
        BLOB_COLORS.forEachIndexed { i, color ->
            blobShaders[i] = RadialGradient(
                0f, 0f, radius,
                intArrayOf(withAlpha(color, 0.55f), withAlpha(color, 0f)),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        basePaint.shader = baseShader
        canvas.drawRect(0f, 0f, w, h, basePaint)

        BLOB_COLORS.indices.forEach { i ->
            val shader = blobShaders[i] ?: return@forEach
            val phase = i * 1.7
            val speed = 0.13 + i * 0.035
            val cx = w * (0.5f + 0.36f * Math.sin(time * speed + phase).toFloat())
            val cy = h * (0.5f + 0.30f * Math.cos(time * speed * 0.85 + phase * 1.6).toFloat())
            blobPaint.shader = shader
            canvas.save()
            canvas.translate(cx, cy)
            canvas.drawCircle(0f, 0f, radius, blobPaint)
            canvas.restore()
        }
    }

    private fun withAlpha(color: Int, alpha: Float): Int =
        Color.argb((alpha * 255).toInt(), Color.red(color), Color.green(color), Color.blue(color))

    companion object {
        private const val BASE_TOP = 0xFF0B1026.toInt()
        private const val BASE_BOTTOM = 0xFF05060E.toInt()
        private val BLOB_COLORS = intArrayOf(
            0xFF7C4DFF.toInt(),
            0xFF00E5C0.toInt(),
            0xFFFF6B9A.toInt(),
            0xFF4D7CFF.toInt()
        )
    }
}
