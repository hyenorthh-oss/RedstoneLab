package com.redstonelab.app.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.redstonelab.app.R
import kotlin.random.Random

/**
 * Gerenciador nativo de efeitos sonoros com SoundPool de baixa latência (sem música de fundo).
 * Carrega arquivos WAV puros sintetizados em res/raw/ com controle de mudo em SharedPreferences.
 */
object SoundManager {

    private var soundPool: SoundPool? = null
    private var soundPlace: Int = 0
    private var soundRemove: Int = 0
    private var soundClick: Int = 0
    private var soundPiston: Int = 0
    private var soundUi: Int = 0

    private var prefs: SharedPreferences? = null
    private const val PREFS_NAME = "redstone_lab_audio"
    private const val KEY_MUTED = "is_muted"

    var isMuted by mutableStateOf(false)
        private set

    fun initialize(context: Context) {
        if (soundPool != null) return

        try {
            val appContext = context.applicationContext
            prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            isMuted = prefs?.getBoolean(KEY_MUTED, false) ?: false

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val pool = SoundPool.Builder()
                .setMaxStreams(6)
                .setAudioAttributes(audioAttributes)
                .build()

            soundPool = pool
            soundPlace = pool.load(appContext, R.raw.sfx_place, 1)
            soundRemove = pool.load(appContext, R.raw.sfx_remove, 1)
            soundClick = pool.load(appContext, R.raw.sfx_click, 1)
            soundPiston = pool.load(appContext, R.raw.sfx_piston, 1)
            soundUi = pool.load(appContext, R.raw.sfx_ui, 1)
        } catch (_: Throwable) {
            // Suprime silenciosamente em testes de JVM headless ou sem hardware de áudio
        }
    }

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        prefs?.edit()?.putBoolean(KEY_MUTED, isMuted)?.apply()
        return isMuted
    }

    fun playPlace() {
        if (isMuted) return
        val pool = soundPool ?: return
        if (soundPlace == 0) return
        val pitch = 0.95f + Random.nextFloat() * 0.10f
        try {
            pool.play(soundPlace, 0.9f, 0.9f, 1, 0, pitch)
        } catch (_: Throwable) {}
    }

    fun playRemove() {
        if (isMuted) return
        val pool = soundPool ?: return
        if (soundRemove == 0) return
        val pitch = 0.95f + Random.nextFloat() * 0.10f
        try {
            pool.play(soundRemove, 0.9f, 0.9f, 1, 0, pitch)
        } catch (_: Throwable) {}
    }

    fun playClick() {
        if (isMuted) return
        val pool = soundPool ?: return
        if (soundClick == 0) return
        try {
            pool.play(soundClick, 0.9f, 0.9f, 1, 0, 1.0f)
        } catch (_: Throwable) {}
    }

    fun playPiston() {
        if (isMuted) return
        val pool = soundPool ?: return
        if (soundPiston == 0) return
        try {
            pool.play(soundPiston, 0.95f, 0.95f, 1, 0, 1.0f)
        } catch (_: Throwable) {}
    }

    fun playUi() {
        if (isMuted) return
        val pool = soundPool ?: return
        if (soundUi == 0) return
        try {
            pool.play(soundUi, 0.8f, 0.8f, 1, 0, 1.0f)
        } catch (_: Throwable) {}
    }

    fun release() {
        try {
            soundPool?.release()
        } catch (_: Throwable) {}
        soundPool = null
        soundPlace = 0
        soundRemove = 0
        soundClick = 0
        soundPiston = 0
        soundUi = 0
    }
}
