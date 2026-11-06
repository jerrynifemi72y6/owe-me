package com.jerrynifemi.oweme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.Choreographer
import android.view.View

/**
 * The drifting colour field the glass sits on. Honours the chosen look: dark, light,
 * a photo backdrop, a frozen field, and how much blur the blobs get.
 */
class LiquidBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val basePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val blobPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val photoPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var baseShader: Shader? = null
    private var blobShaders = arrayOfNulls<RadialGradient>(4)
    private var radius = 0f
    private var time = 0.0
    private var pulse = true
    private var light = false
    private var accent = 0xFF7C4DFF.toInt()
    private var photo: Bitmap? = null
    private var solid = false
    private val photoRect = RectF()

    private val ticker = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (pulse) {
                time = frameTimeNanos / 1_000_000_000.0
                invalidate()
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
    }

    init {
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            setRenderEffect(
                android.graphics.RenderEffect.createBlurEffect(48f, 48f, Shader.TileMode.CLAMP)
            )
        }
    }

    fun configure(context: Context, settings: Settings) {
        val skin = Skin.of(settings)
        light = settings.isLight
        accent = skin.accent
        pulse = skin.pulse
        solid = !settings.isGlass
        photo = if (solid) null
        else Backdrop.load(context, settings.backdrop, Math.max(width, height).coerceAtLeast(1080))
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            setRenderEffect(
                if (settings.blur <= 0) null
                else android.graphics.RenderEffect.createBlurEffect(
                    settings.blur.toFloat(), settings.blur.toFloat(), Shader.TileMode.CLAMP
                )
            )
        }
        rebuild()
        invalidate()
    }

    private fun rebuild() {
        val w = width
        val h = height
        if (w <= 0 || h <= 0) return
        val top = if (light) 0xFFFFF7F1.toInt() else 0xFF0B1026.toInt()
        val bottom = if (light) 0xFFE9EEFA.toInt() else 0xFF05060E.toInt()
        baseShader = LinearGradient(
            0f, 0f, w * 0.35f, h.toFloat(),
            intArrayOf(top, bottom), null, Shader.TileMode.CLAMP
        )
        radius = (Math.min(w, h) * 0.62f).coerceAtLeast(160f)
        val colors = Backdrop.blobs(accent, light)
        val strength = if (light) 0.38f else 0.55f
        colors.forEachIndexed { i, color ->
            blobShaders[i] = RadialGradient(
                0f, 0f, radius,
                intArrayOf(Backdrop.withAlpha(color, strength), Backdrop.withAlpha(color, 0f)),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
            )
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
        rebuild()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val picture = photo
        if (picture != null) {
            // centre-crop the photo behind everything, then dim it for legibility
            val scale = Math.max(w / picture.width, h / picture.height)
            val dw = picture.width * scale
            val dh = picture.height * scale
            photoRect.set((w - dw) / 2f, (h - dh) / 2f, (w + dw) / 2f, (h + dh) / 2f)
            canvas.drawBitmap(picture, null, photoRect, photoPaint)
            scrimPaint.color = if (light) Color.argb(120, 255, 255, 255) else Color.argb(150, 5, 6, 14)
            canvas.drawRect(0f, 0f, w, h, scrimPaint)
        } else {
            basePaint.shader = baseShader
            canvas.drawRect(0f, 0f, w, h, basePaint)
        }

        if (solid) return

        for (i in blobShaders.indices) {
            val shader = blobShaders[i] ?: continue
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
}
