package com.jerrynifemi.oweme

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/** Three tiny glass sounds. Nothing else in the app makes noise. */
object Sound {

    private var pool: SoundPool? = null
    private val ids = HashMap<Int, Int>()

    private val sounds = intArrayOf(R.raw.sfx_tap, R.raw.sfx_add, R.raw.sfx_settle)

    private fun ensure(context: Context): SoundPool {
        pool?.let { return it }
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val sp = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build()
        sounds.forEach { ids[it] = sp.load(context.applicationContext, it, 1) }
        pool = sp
        return sp
    }

    fun play(context: Context, resId: Int) {
        val sp = ensure(context)
        val id = ids[resId] ?: return
        sp.play(id, 0.9f, 0.9f, 1, 0, 1f)
    }
}
