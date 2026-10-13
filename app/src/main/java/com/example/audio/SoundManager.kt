package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

class SoundManager(private val context: Context) {
  private val scope = CoroutineScope(Dispatchers.Default)
  var isAudioEnabled: Boolean = true
  var isHapticsEnabled: Boolean = true

  private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
    vibratorManager?.defaultVibrator
  } else {
    @Suppress("DEPRECATION")
    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
  }

  fun vibrate(durationMs: Long, amplitude: Int = 180) {
    if (!isHapticsEnabled || vibrator == null || !vibrator.hasVibrator()) return
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val amp = amplitude.coerceIn(1, 255)
        vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amp))
      } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(durationMs)
      }
    } catch (_: Exception) {}
  }

  fun playCountdownBeep(isFinal: Boolean) {
    if (!isAudioEnabled) return
    scope.launch {
      val sampleRate = 22050
      val durationMs = if (isFinal) 400 else 180
      val numSamples = (durationMs * sampleRate) / 1000
      val freq = if (isFinal) 880f else 440f
      val buffer = ShortArray(numSamples)

      for (i in 0 until numSamples) {
        val t = i.toFloat() / sampleRate
        val envelope = (1f - (i.toFloat() / numSamples)).coerceIn(0f, 1f)
        val sample = (sin(2.0 * Math.PI * freq * t) * 32000.0 * envelope).toInt()
        buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
      }
      playPcmBuffer(buffer, sampleRate)
    }
  }

  fun playCrash() {
    vibrate(120, 255)
    if (!isAudioEnabled) return
    scope.launch {
      val sampleRate = 22050
      val durationMs = 250
      val numSamples = (durationMs * sampleRate) / 1000
      val buffer = ShortArray(numSamples)
      val rnd = Random(42)

      for (i in 0 until numSamples) {
        val envelope = (1f - (i.toFloat() / numSamples)).coerceIn(0f, 1f)
        val noise = (rnd.nextFloat() * 2f - 1f)
        val bassFreq = 60f + 120f * (1f - i.toFloat() / numSamples)
        val bass = sin(2.0 * Math.PI * bassFreq * (i.toFloat() / sampleRate))
        val sample = ((noise * 0.6f + bass * 0.4f) * 30000.0 * envelope).toInt()
        buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
      }
      playPcmBuffer(buffer, sampleRate)
    }
  }

  fun playNitroBoost() {
    vibrate(80, 200)
    if (!isAudioEnabled) return
    scope.launch {
      val sampleRate = 22050
      val durationMs = 350
      val numSamples = (durationMs * sampleRate) / 1000
      val buffer = ShortArray(numSamples)
      val rnd = Random(77)

      for (i in 0 until numSamples) {
        val t = i.toFloat() / numSamples
        val envelope = (sin(t * Math.PI)).toFloat().coerceIn(0f, 1f)
        val freq = 200f + 600f * t
        val tone = sin(2.0 * Math.PI * freq * (i.toFloat() / sampleRate))
        val roar = (rnd.nextFloat() * 2f - 1f) * 0.5f
        val sample = ((tone * 0.5f + roar) * 28000.0 * envelope).toInt()
        buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
      }
      playPcmBuffer(buffer, sampleRate)
    }
  }

  fun playDriftScreech() {
    vibrate(40, 100)
    if (!isAudioEnabled) return
    scope.launch {
      val sampleRate = 22050
      val durationMs = 120
      val numSamples = (durationMs * sampleRate) / 1000
      val buffer = ShortArray(numSamples)
      val rnd = Random(12)

      for (i in 0 until numSamples) {
        val envelope = (1f - (i.toFloat() / numSamples)).coerceIn(0f, 1f)
        val screech = (rnd.nextFloat() * 2f - 1f) * 0.8f
        val sample = (screech * 20000.0 * envelope).toInt()
        buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
      }
      playPcmBuffer(buffer, sampleRate)
    }
  }

  fun playLapComplete() {
    vibrate(60, 160)
    if (!isAudioEnabled) return
    scope.launch {
      val sampleRate = 22050
      val notes = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f) // C5, E5, G5, C6
      val noteDurationMs = 80
      val totalSamples = (notes.size * noteDurationMs * sampleRate) / 1000
      val buffer = ShortArray(totalSamples)

      for (n in notes.indices) {
        val noteFreq = notes[n]
        val startSample = (n * noteDurationMs * sampleRate) / 1000
        val endSample = ((n + 1) * noteDurationMs * sampleRate) / 1000
        val noteLen = endSample - startSample
        for (i in startSample until endSample) {
          val rel = (i - startSample).toFloat() / noteLen
          val env = (1f - rel).coerceIn(0f, 1f)
          val t = i.toFloat() / sampleRate
          val sample = (sin(2.0 * Math.PI * noteFreq * t) * 28000.0 * env).toInt()
          buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
      }
      playPcmBuffer(buffer, sampleRate)
    }
  }

  private fun playPcmBuffer(buffer: ShortArray, sampleRate: Int) {
    try {
      val track = AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        )
        .setBufferSizeInBytes(buffer.size * 2)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()

      track.write(buffer, 0, buffer.size)
      track.play()
      // Release after playback finishes
      scope.launch {
        kotlinx.coroutines.delay((buffer.size * 1000L / sampleRate) + 50L)
        try {
          track.stop()
          track.release()
        } catch (_: Exception) {}
      }
    } catch (_: Exception) {}
  }
}
