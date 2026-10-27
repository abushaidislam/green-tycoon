package com.greentycoon.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.greentycoon.game.R
import java.util.concurrent.ConcurrentHashMap

object SoundManager {
    @Volatile
    private var soundPool: SoundPool? = null
    private val soundMap = ConcurrentHashMap<Class<out GameSoundEvent>, Int>()

    @Volatile
    private var isMuted = false

    @Synchronized
    fun init(context: Context) {
        if (soundPool != null) return

        val appContext = context.applicationContext

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attrs)
            .build().apply {
                loadSound(appContext, GameSoundEvent.Water::class.java, R.raw.water, R.raw.watering)
                loadSound(appContext, GameSoundEvent.Harvest::class.java, R.raw.cut)
                loadSound(appContext, GameSoundEvent.Fertilize::class.java, R.raw.fertilize, R.raw.ds)
                loadSound(appContext, GameSoundEvent.CashEarned::class.java, R.raw.earn)
                loadSound(appContext, GameSoundEvent.DoorKnock::class.java, R.raw.bell)
                loadSound(appContext, GameSoundEvent.DoorOpen::class.java, R.raw.door_open)
                loadSound(appContext, GameSoundEvent.DoorClose::class.java, R.raw.door_close)
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

    @Synchronized
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

    @Synchronized
    fun release() {
        val pool = soundPool
        soundPool = null
        soundMap.clear()
        pool?.release()
    }
}
