package com.jerrynifemi.oweme

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView

/** Short glassy intro: the mark springs in, the name rises, then the ledger opens. */
class SplashActivity : Activity() {

    private var launched = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val settings = Settings(this)
        Money.symbol = settings.currency

        val root = findViewById<View>(R.id.splashRoot)
        val background = findViewById<LiquidBackgroundView>(R.id.splashBg)
        val badge = findViewById<View>(R.id.splashBadge)
        val word = findViewById<TextView>(R.id.splashWord)
        val tag = findViewById<TextView>(R.id.splashTag)

        background.configure(this, settings)
        Skin.apply(root, settings)

        badge.scaleX = 0.55f
        badge.scaleY = 0.55f
        badge.alpha = 0f
        word.alpha = 0f
        word.translationY = dp(20).toFloat()
        tag.alpha = 0f

        badge.animate()
            .scaleX(1f).scaleY(1f).alpha(1f)
            .setDuration(640)
            .setInterpolator(Motion.spring(0.62f, 1.7f))
            .start()

        word.animate()
            .alpha(1f).translationY(0f)
            .setStartDelay(170)
            .setDuration(560)
            .setInterpolator(Motion.spring(0.78f, 1.8f))
            .start()

        tag.animate()
            .alpha(1f)
            .setStartDelay(420)
            .setDuration(420)
            .start()

        root.setOnClickListener { open() }
        handler.postDelayed({ open() }, 1650)
    }

    private fun open() {
        if (launched) return
        launched = true
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
