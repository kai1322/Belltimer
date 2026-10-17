package com.belltimer

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.SystemClock
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/** สร้างเสียงระฆัง "ปิ๊ง ปิ๊ง" ด้วยโค้ด (additive synthesis) ไม่ใช้ไฟล์เสียง */
object BellPlayer {
    private const val SR = 44100
    private const val BASE = 1760.0
    private val RATIOS = doubleArrayOf(1.0, 2.0, 2.76, 5.4)
    private val AMPS = doubleArrayOf(1.0, 0.45, 0.30, 0.12)
    private val TAUS = doubleArrayOf(0.30, 0.18, 0.10, 0.05)

    private const val DING_LEN = 1.0      // วินาทีต่อหนึ่ง "ปิ๊ง"
    private const val DING_GAP = 0.38     // ระยะระหว่าง ปิ๊ง แรกกับที่สอง
    private const val ROUND_GAP = 1.9     // ระยะระหว่างรอบ

    @Volatile private var gen = 0

    fun stop() { gen++ }

    /** เล่นแบบ blocking จนจบ (เรียกจาก thread พื้นหลัง) */
    fun play(repeat: Int, volumePercent: Int) {
        val my = ++gen
        val pcm = build(repeat.coerceIn(1, 3))
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SR)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        try {
            track.write(pcm, 0, pcm.size)
            track.setVolume((volumePercent / 100f).toDouble().pow(2.0).toFloat())
            track.play()
            val end = SystemClock.elapsedRealtime() + pcm.size * 1000L / SR + 150
            while (gen == my && SystemClock.elapsedRealtime() < end) {
                Thread.sleep(40)
            }
            track.stop()
        } catch (e: Exception) {
            // ignore
        } finally {
            track.release()
        }
    }

    private fun build(count: Int): ShortArray {
        val totalSec = (count - 1) * ROUND_GAP + DING_GAP + DING_LEN + 0.1
        val mix = DoubleArray((totalSec * SR).toInt())
        for (r in 0 until count) {
            addDing(mix, r * ROUND_GAP)
            addDing(mix, r * ROUND_GAP + DING_GAP)
        }
        var peak = 1e-9
        for (x in mix) if (abs(x) > peak) peak = abs(x)
        val scale = 0.9 / peak * 32767.0
        return ShortArray(mix.size) { (mix[it] * scale).toInt().toShort() }
    }

    private fun addDing(buf: DoubleArray, startSec: Double) {
        val s0 = (startSec * SR).toInt()
        val len = (DING_LEN * SR).toInt()
        val fadeLen = 882 // 20 ms
        for (i in 0 until len) {
            val idx = s0 + i
            if (idx >= buf.size) break
            val t = i.toDouble() / SR
            var v = 0.0
            for (k in RATIOS.indices) {
                v += AMPS[k] * sin(2.0 * PI * BASE * RATIOS[k] * t) * exp(-t / TAUS[k])
            }
            val attack = 1.0 - exp(-t / 0.002)
            val fade = if (i > len - fadeLen) (len - i).toDouble() / fadeLen else 1.0
            buf[idx] += v * attack * fade
        }
    }
}
