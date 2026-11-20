package com.redstonelab.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.redstonelab.app.engine.RedstoneSoundEvent
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class SoundEffect {
    BLOCK_PLACE,
    BLOCK_BREAK,
    LEVER_CLICK,
    BUTTON_CLICK,
    PISTON_EXTEND,
    PISTON_RETRACT,
    NOTE_BLOCK,
    BELL,
    TNT_PRIME,
    DOOR_TOGGLE,
    UI_TAP
}

/**
 * Gerenciador nativo de efeitos sonoros com SoundPool de baixa latência (sem música de fundo).
 * Sintetiza áudios PCM 16-bit 22050Hz procedurais autênticos do Minecraft Redstone.
 */
object SoundManager {

    private var soundPool: SoundPool? = null
    private val soundIds = HashMap<SoundEffect, Int>()
    private var isInitialized = false

    var isMuted by mutableStateOf(false)
        private set

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        return isMuted
    }

    fun initialize(context: Context) {
        if (isInitialized) return

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val pool = SoundPool.Builder()
                .setMaxStreams(8)
                .setAudioAttributes(audioAttributes)
                .build()

            soundPool = pool

            val sfxDir = File(context.cacheDir, "sfx").apply { mkdirs() }

            for (effect in SoundEffect.entries) {
                val samples = generateSamples(effect)
                val wavFile = File(sfxDir, "${effect.name.lowercase()}.wav")
                writeWavFile(wavFile, samples, 22050)
                val soundId = pool.load(wavFile.absolutePath, 1)
                soundIds[effect] = soundId
            }

            isInitialized = true
        } catch (_: Throwable) {
            // Suprime silenciosamente em ambientes de teste JVM ou sem hardware de áudio
        }
    }

    fun play(effect: SoundEffect, volume: Float = 0.9f) {
        if (isMuted) return
        val pool = soundPool ?: return
        val id = soundIds[effect] ?: return
        try {
            pool.play(id, volume, volume, 1, 0, 1.0f)
        } catch (_: Throwable) {}
    }

    fun play(event: RedstoneSoundEvent, volume: Float = 0.9f) {
        val effect = when (event) {
            RedstoneSoundEvent.PISTON_EXTEND -> SoundEffect.PISTON_EXTEND
            RedstoneSoundEvent.PISTON_RETRACT -> SoundEffect.PISTON_RETRACT
            RedstoneSoundEvent.NOTE_BLOCK -> SoundEffect.NOTE_BLOCK
            RedstoneSoundEvent.BELL -> SoundEffect.BELL
            RedstoneSoundEvent.TNT_PRIME -> SoundEffect.TNT_PRIME
            RedstoneSoundEvent.DOOR_TOGGLE -> SoundEffect.DOOR_TOGGLE
            RedstoneSoundEvent.COPPER_BULB_TOGGLE -> SoundEffect.LEVER_CLICK
        }
        play(effect, volume)
    }

    private fun generateSamples(effect: SoundEffect): ShortArray {
        val sampleRate = 22050
        return when (effect) {
            SoundEffect.LEVER_CLICK -> {
                // Clique duplo mecânico característico de alavanca
                val count = (sampleRate * 0.040).toInt()
                val samples = ShortArray(count)
                for (i in 0 until count) {
                    val t = i.toDouble() / sampleRate
                    val s1 = if (t < 0.015) sin(2 * PI * 2400 * t) * exp(-t * 400.0) else 0.0
                    val s2 = if (t >= 0.015) sin(2 * PI * 1800 * (t - 0.015)) * exp(-(t - 0.015) * 350.0) else 0.0
                    samples[i] = ((s1 + s2) * 26000.0).toInt().coerceIn(-32767, 32767).toShort()
                }
                samples
            }

            SoundEffect.BUTTON_CLICK -> {
                // Clique amadeirado / pedra
                val count = (sampleRate * 0.045).toInt()
                val samples = ShortArray(count)
                for (i in 0 until count) {
                    val t = i.toDouble() / sampleRate
                    val s = sin(2 * PI * 950 * t) * exp(-t * 160.0)
                    samples[i] = (s * 27000.0).toInt().coerceIn(-32767, 32767).toShort()
                }
                samples
            }

            SoundEffect.PISTON_EXTEND -> {
                // Golpe pneumático grave com ruído de impulso
                val count = (sampleRate * 0.13).toInt()
                val samples = ShortArray(count)
                for (i in 0 until count) {
                    val t = i.toDouble() / sampleRate
                    val freq = 160.0 - (105.0 * (t / 0.13))
                    val noise = ((i * 1103515245 + 12345) % 65536 - 32768) / 32768.0
                    val s = (sin(2 * PI * freq * t) * 0.75 + noise * 0.25) * exp(-t * 16.0)
                    samples[i] = (s * 28000.0).toInt().coerceIn(-32767, 32767).toShort()
                }
                samples
            }

            SoundEffect.PISTON_RETRACT -> {
                // Estalo seco pneumático
                val count = (sampleRate * 0.07).toInt()
                val samples = ShortArray(count)
                for (i in 0 until count) {
                    val t = i.toDouble() / sampleRate
                    val freq = 220.0 - (130.0 * (t / 0.07))
                    val s = sin(2 * PI * freq * t) * exp(-t * 32.0)
                    samples[i] = (s * 26000.0).toInt().coerceIn(-32767, 32767).toShort()
                }
                samples
            }

            SoundEffect.BLOCK_PLACE -> {
                // Toque de colocação de pedra / pó
                val count = (sampleRate * 0.05).toInt()
                val samples = ShortArray(count)
                for (i in 0 until count) {
                    val t = i.toDouble() / sampleRate
                    val freq = 380.0 - (200.0 * (t / 0.05))
                    val noise = ((i * 37199 + 17) % 65536 - 32768) / 32768.0
                    val s = (sin(2 * PI * freq * t) * 0.6 + noise * 0.4) * exp(-t * 38.0)
                    samples[i] = (s * 25000.0).toInt().coerceIn(-32767, 32767).toShort()
                }
                samples
            }

            SoundEffect.BLOCK_BREAK -> {
                // Ruído crocante de quebra de bloco
                val count = (sampleRate * 0.08).toInt()
                val samples = ShortArray(count)
                for (i in 0 until count) {
                    val t = i.toDouble() / sampleRate
                    val noise = ((i * 1234567 + 987654321) % 65536 - 32768) / 32768.0
                    val envelope = exp(-t * 35.0) * (1.0 - exp(-t * 600.0))
                    val s = noise * envelope
                    samples[i] = (s * 27000.0).toInt().coerceIn(-32767, 32767).toShort()
                }
                samples
            }

            SoundEffect.NOTE_BLOCK -> {
                // Nota musical harmônica A4 (440Hz + 880Hz)
                val count = (sampleRate * 0.35).toInt()
                val samples = ShortArray(count)
                for (i in 0 until count) {
                    val t = i.toDouble() / sampleRate
                    val fund = sin(2 * PI * 440.0 * t) * 0.72
                    val harm = sin(2 * PI * 880.0 * t) * 0.28
                    val s = (fund + harm) * exp(-t * 7.5)
                    samples[i] = (s * 27000.0).toInt().coerceIn(-32767, 32767).toShort()
                }
                samples
            }

            SoundEffect.BELL -> {
                // Sino metálico com múltiplos harmônicos
                val count = (sampleRate * 0.50).toInt()
                val samples = ShortArray(count)
                for (i in 0 until count) {
                    val t = i.toDouble() / sampleRate
                    val h1 = sin(2 * PI * 1100.0 * t) * 0.50
                    val h2 = sin(2 * PI * 1550.0 * t) * 0.30
                    val h3 = sin(2 * PI * 2200.0 * t) * 0.20
                    val s = (h1 + h2 + h3) * exp(-t * 5.0)
                    samples[i] = (s * 27000.0).toInt().coerceIn(-32767, 32767).toShort()
                }
                samples
            }

            SoundEffect.TNT_PRIME -> {
                // Chiado de pavio de TNT
                val count = (sampleRate * 0.15).toInt()
                val samples = ShortArray(count)
                for (i in 0 until count) {
                    val t = i.toDouble() / sampleRate
                    val noise = ((i * 71329 + 131) % 65536 - 32768) / 32768.0
                    val s = noise * exp(-t * 12.0)
                    samples[i] = (s * 20000.0).toInt().coerceIn(-32767, 32767).toShort()
                }
                samples
            }

            SoundEffect.DOOR_TOGGLE -> {
                // Batida metálica de porta de ferro
                val count = (sampleRate * 0.07).toInt()
                val samples = ShortArray(count)
                for (i in 0 until count) {
                    val t = i.toDouble() / sampleRate
                    val freq = 310.0 - (150.0 * (t / 0.07))
                    val s = sin(2 * PI * freq * t) * exp(-t * 26.0)
                    samples[i] = (s * 25000.0).toInt().coerceIn(-32767, 32767).toShort()
                }
                samples
            }

            SoundEffect.UI_TAP -> {
                // Micro-clique de interface (12ms)
                val count = (sampleRate * 0.012).toInt()
                val samples = ShortArray(count)
                for (i in 0 until count) {
                    val t = i.toDouble() / sampleRate
                    val s = sin(2 * PI * 1400.0 * t) * exp(-t * 300.0)
                    samples[i] = (s * 19000.0).toInt().coerceIn(-32767, 32767).toShort()
                }
                samples
            }
        }
    }

    private fun writeWavFile(file: File, samples: ShortArray, sampleRate: Int) {
        val numSamples = samples.size
        val dataSize = numSamples * 2
        val totalSize = 36 + dataSize

        FileOutputStream(file).use { fos ->
            val header = ByteBuffer.allocate(44).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                put("RIFF".toByteArray())
                putInt(totalSize)
                put("WAVE".toByteArray())
                put("fmt ".toByteArray())
                putInt(16) // Subchunk1Size (16 for PCM)
                putShort(1) // AudioFormat (1 for PCM)
                putShort(1) // NumChannels (1: Mono)
                putInt(sampleRate)
                putInt(sampleRate * 2) // ByteRate = SampleRate * NumChannels * BitsPerSample/8
                putShort(2) // BlockAlign = NumChannels * BitsPerSample/8
                putShort(16) // BitsPerSample
                put("data".toByteArray())
                putInt(dataSize)
            }
            fos.write(header.array())

            val sampleBuffer = ByteBuffer.allocate(dataSize).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                for (sample in samples) {
                    putShort(sample)
                }
            }
            fos.write(sampleBuffer.array())
        }
    }
}
