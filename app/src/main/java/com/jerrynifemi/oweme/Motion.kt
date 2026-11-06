package com.jerrynifemi.oweme

import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.Interpolator
import android.view.animation.PathInterpolator
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Damped-spring motion. Everything in this app moves with real physics instead of
 * the default builder ease, which is what makes it feel liquid rather than flat.
 */
class SpringInterpolator(
    private val damping: Float = 0.62f,
    private val frequency: Float = 1.9f
) : Interpolator {

    override fun getInterpolation(t: Float): Float {
        if (t <= 0f) return 0f
        if (t >= 1f) return 1f
        val w = (2.0 * Math.PI * frequency).toFloat()
        val z = damping.toDouble()
        return if (z < 1.0) {
            val wd = (w * sqrt((1.0 - z * z).toFloat())).toFloat()
            val e = exp((-z * w * t).toDouble()).toFloat()
            (1f - e * (cos(wd * t) + ((z * w / wd).toFloat() * sin(wd * t)))).toFloat()
        } else {
            (1f - exp((-w * t).toDouble()) * (1f + w * t)).toFloat()
        }
    }
}

object Motion {

    fun spring(damping: Float = 0.62f, frequency: Float = 1.9f): Interpolator =
        SpringInterpolator(damping, frequency)

    /** Skewed ease-out tuned by eye: quick start, long settle, no bounce. */
    fun glide(): Interpolator = PathInterpolator(0.22f, 0.61f, 0.24f, 1f)

    /** Press-in scale plus haptic, spring back on release. Click handling stays intact. */
    fun View.springyPress(scaleTo: Float = 0.968f) {
        setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().cancel()
                    v.animate().scaleX(scaleTo).scaleY(scaleTo)
                        .setDuration(120).setInterpolator(glide()).start()
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().cancel()
                    v.animate().scaleX(1f).scaleY(1f)
                        .setDuration(420).setInterpolator(spring(0.55f, 2.1f)).start()
                }
            }
            false
        }
    }

    /** Fade + rise entrance. [stagger] pushes each item a little further behind the last. */
    fun View.enter(delay: Long = 0L, riseDp: Float = 18f, duration: Long = 520L) {
        val rise = riseDp * resources.displayMetrics.density
        alpha = 0f
        translationY = rise
        animate().alpha(1f).translationY(0f)
            .setStartDelay(delay)
            .setDuration(duration)
            .setInterpolator(spring(0.72f, 1.6f))
            .start()
    }

    fun View.countUp(from: Long, to: Long, duration: Long = 700L, onValue: (Long) -> Unit) {
        val start = System.currentTimeMillis()
        val interpolator = SpringInterpolator(0.8f, 1.5f)
        val frame = object : Runnable {
            override fun run() {
                val progress = ((System.currentTimeMillis() - start).toFloat() / duration).coerceIn(0f, 1f)
                val eased = interpolator.getInterpolation(progress)
                onValue(from + ((to - from) * eased).toLong())
                if (progress < 1f) postOnAnimation(this)
            }
        }
        postOnAnimation(frame)
    }

    fun View.shake() {
        val step = 7f * resources.displayMetrics.density
        animate().cancel()
        animate().translationX(step).setDuration(60).withEndAction {
            animate().translationX(-step * 0.7f).setDuration(60).withEndAction {
                animate().translationX(step * 0.4f).setDuration(60).withEndAction {
                    animate().translationX(0f).setDuration(90).start()
                }.start()
            }.start()
        }.start()
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    fun nearlyEqual(a: Float, b: Float): Boolean = abs(a - b) < 0.01f
}
