package com.jerrynifemi.oweme

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.view.ViewGroup
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** A short burst of paper, thrown when a debt gets settled. Removes itself. */
class ConfettiView(context: Context) : View(context) {

    private class Bit(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var rot: Float,
        var spin: Float,
        val color: Int,
        val w: Float,
        val h: Float
    )

    private val bits = ArrayList<Bit>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var last = 0L
    private var age = 0f
    private val density = resources.displayMetrics.density

    fun burst(x: Float, y: Float, accent: Int) {
        val palette = intArrayOf(
            accent, 0xFF00E5C0.toInt(), 0xFFFF6B9A.toInt(), 0xFFFFC44D.toInt(), 0xFFFFFFFF.toInt()
        )
        for (i in 0 until 54) {
            val angle = Random.nextFloat() * 6.2832f
            val speed = (240f + Random.nextFloat() * 620f) * density
            bits.add(
                Bit(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed - 260f * density,
                    rot = Random.nextFloat() * 360f,
                    spin = (Random.nextFloat() - 0.5f) * 720f,
                    color = palette[i % palette.size],
                    w = (5f + Random.nextFloat() * 6f) * density,
                    h = (9f + Random.nextFloat() * 8f) * density
                )
            )
        }
        last = 0L
        age = 0f
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        val now = System.nanoTime()
        val dt = if (last == 0L) 0.016f else ((now - last) / 1_000_000_000.0).toFloat()
        last = now
        val step = dt.coerceAtMost(0.04f)
        age += step

        bits.forEach { b ->
            b.vy += 1900f * density * step
            b.vx *= 0.994f
            b.x += b.vx * step
            b.y += b.vy * step
            b.rot += b.spin * step
        }
        bits.removeAll { it.y > height + 80f || it.x < -120f || it.x > width + 120f }

        bits.forEach { b ->
            canvas.save()
            canvas.translate(b.x, b.y)
            canvas.rotate(b.rot)
            paint.color = b.color
            canvas.drawRect(-b.w / 2f, -b.h / 2f, b.w / 2f, b.h / 2f, paint)
            canvas.restore()
        }

        if (bits.isNotEmpty() && age < 3.2f) postInvalidateOnAnimation()
        else (parent as? ViewGroup)?.removeView(this)
    }
}
