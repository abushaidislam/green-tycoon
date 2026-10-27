package com.greentycoon.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.greentycoon.game.R

object SoundManager {
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<Class<out GameSoundEvent>, Int>()
    private var isMuted = false

    fun init(context: Context) {
        if (soundPool != null) return

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attrs)
            .build().apply {
                loadSound(context, GameSoundEvent.Water::class.java, R.raw.water, R.raw.watering)
                loadSound(context, GameSoundEvent.Harvest::class.java, R.raw.cut)
                loadSound(context, GameSoundEvent.Fertilize::class.java, R.raw.fertilize, R.raw.ds)
                loadSound(context, GameSoundEvent.CashEarned::class.java, R.raw.earn)
                loadSound(context, GameSoundEvent.DoorKnock::class.java, R.raw.bell)
                loadSound(context, GameSoundEvent.DoorOpen::class.java, R.raw.door_open)
                loadSound(context, GameSoundEvent.DoorClose::class.java, R.raw.door_close)
            }
    }

    private fun SoundPool.loadSound(context: Context, key: Class<out GameSoundEvent>, vararg resIds: Int) {
        for (resId in resIds) {
            try {
                val soundId = load(context, resId, 1)
                if (soundId != 0) {
                    soundMap[key] = soundId
                    return
                }
            } catch (_: Exception) {}
        }
    }

    fun play(event: GameSoundEvent) {
        if (isMuted) return
        val soundId = soundMap[event::class.java] ?: return
        soundPool?.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
    }

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        return isMuted
    }

    fun isMuted(): Boolean = isMuted

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        soundMap.clear()
    }
}
