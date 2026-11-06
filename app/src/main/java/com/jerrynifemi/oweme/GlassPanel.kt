package com.jerrynifemi.oweme

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout

/**
 * A sheet of frosted glass drawn by hand: translucent fill, bright top sheen,
 * hairline border, and a specular streak that drifts across it. Set [living] on a
 * panel to keep the streak slowly moving, which is what sells the "liquid" idea.
 */
class GlassPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    var cornerRadius: Float = dp(26f)
        set(value) { field = value; updatePath(); invalidate() }

    var tintColor: Int = Color.parseColor("#1AFFFFFF")
        set(value) { field = value; invalidate() }

    var borderColor: Int = Color.parseColor("#3DFFFFFF")
        set(value) { field = value; invalidate() }

    /** Slowly drifting specular highlight. Use on hero panels only, it costs a loop. */
    /** When false the panel is a flat card: no sheen, no moving highlight. */
    var frosted: Boolean = true
        set(value) {
            field = value
            invalidate()
        }

    var living: Boolean = false
        set(value) {
            field = value
            drift?.cancel()
            drift = if (value) {
                ObjectAnimator.ofFloat(this, "highlight", 0f, 1f).apply {
                    duration = 6200
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.RESTART
                    interpolator = LinearInterpolator()
                    start()
                }
            } else null
        }

    var highlight: Float = 0f
        set(value) { field = value; invalidate() }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
    }
    private val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val specularPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val clip = Path()
    private var sheenShader: Shader? = null
    private var specularShader: Shader? = null
    private var drift: ObjectAnimator? = null

    init {
        setWillNotDraw(false)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rect.set(0f, 0f, w.toFloat(), h.toFloat())
        updatePath()
        sheenShader = LinearGradient(
            0f, 0f, 0f, h * 0.85f,
            intArrayOf(Color.parseColor("#26FFFFFF"), Color.parseColor("#0AFFFFFF"), Color.TRANSPARENT),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP
        )
        val band = (w * 0.55f).coerceAtLeast(1f)
        specularShader = LinearGradient(
            0f, 0f, band, h.toFloat(),
            intArrayOf(Color.TRANSPARENT, Color.parseColor("#33FFFFFF"), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
        )
    }

    private fun updatePath() {
        clip.reset()
        clip.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (rect.width() <= 0f) return

        fillPaint.color = tintColor
        canvas.drawPath(clip, fillPaint)

        if (frosted) sheenShader?.let {
            sheenPaint.shader = it
            canvas.drawPath(clip, sheenPaint)
        }

        if (frosted) specularShader?.let {
            val band = (width * 0.55f).coerceAtLeast(1f)
            val travel = width + band
            val x = -band + travel * highlight
            canvas.save()
            canvas.clipPath(clip)
            canvas.rotate(-18f, width / 2f, height / 2f)
            canvas.translate(x, 0f)
            specularPaint.shader = it
            canvas.drawRect(0f, 0f, band, height.toFloat(), specularPaint)
            canvas.restore()
        }

        borderPaint.color = borderColor
        canvas.drawPath(clip, borderPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        drift?.cancel()
        drift = null
        animate().cancel()
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
