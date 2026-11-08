// Заголовок WAV и разбор PCM — байтовая арифметика с фиксированными смещениями формата.
@file:Suppress("MagicNumber")

package ru.nuclearhack.stenographer.feature.home.presentation.record

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.sqrt

/**
 * Запись с микрофона сразу в WAV 16 кГц моно 16 бит — родной формат конвейера, без перекодирования.
 * [paused] читается на каждом блоке: на паузе микрофон открыт, но звук не пишется.
 */
internal class WavRecorder(private val file: File) {
    @Volatile var paused = false

    @Volatile var stopRequested = false

    /** Пишет до [stopRequested]; [onLevel] получает уровень 0..1 и длительность записанного в мс. */
    @SuppressLint("MissingPermission") // разрешение проверяет экран перед стартом
    suspend fun record(onLevel: (level: Float, writtenMs: Long) -> Unit) = withContext(Dispatchers.IO) {
        val minBuf =
            AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuf, BLOCK_SAMPLES * 2 * 4),
        )
        check(record.state == AudioRecord.STATE_INITIALIZED) { "Микрофон недоступен" }
        file.parentFile?.mkdirs()
        val out = RandomAccessFile(file, "rw")
        try {
            out.setLength(0)
            out.write(ByteArray(WAV_HEADER))
            val buf = ShortArray(BLOCK_SAMPLES)
            val bytes = ByteArray(BLOCK_SAMPLES * 2)
            var written = 0L
            record.startRecording()
            while (!stopRequested && currentCoroutineContext().isActive) {
                val n = record.read(buf, 0, buf.size)
                if (n <= 0) continue
                var sum = 0.0
                for (i in 0 until n) sum += buf[i].toDouble() * buf[i]
                val rms = sqrt(sum / n) / Short.MAX_VALUE
                if (!paused) {
                    for (i in 0 until n) {
                        bytes[2 * i] = (buf[i].toInt() and 0xFF).toByte()
                        bytes[2 * i + 1] = (buf[i].toInt() shr 8 and 0xFF).toByte()
                    }
                    out.write(bytes, 0, n * 2)
                    written += n
                }
                onLevel((rms * LEVEL_GAIN).toFloat().coerceIn(0f, 1f), written * 1000 / SAMPLE_RATE)
            }
            record.stop()
            writeHeader(out, written)
        } finally {
            record.release()
            out.close()
        }
    }

    private fun writeHeader(out: RandomAccessFile, samples: Long) {
        val dataBytes = samples * 2
        out.seek(0)
        out.writeBytes("RIFF")
        out.writeIntLe((36 + dataBytes).toInt())
        out.writeBytes("WAVE")
        out.writeBytes("fmt ")
        out.writeIntLe(16) // размер блока fmt
        out.writeShortLe(1) // PCM
        out.writeShortLe(1) // моно
        out.writeIntLe(SAMPLE_RATE)
        out.writeIntLe(SAMPLE_RATE * 2) // байт в секунду
        out.writeShortLe(2) // байт на отсчёт
        out.writeShortLe(16) // бит на отсчёт
        out.writeBytes("data")
        out.writeIntLe(dataBytes.toInt())
    }

    private fun RandomAccessFile.writeIntLe(v: Int) =
        write(byteArrayOf(v.toByte(), (v shr 8).toByte(), (v shr 16).toByte(), (v shr 24).toByte()))

    private fun RandomAccessFile.writeShortLe(v: Int) = write(byteArrayOf(v.toByte(), (v shr 8).toByte()))

    companion object {
        const val SAMPLE_RATE = 16_000
        private const val BLOCK_SAMPLES = 1600 // 100 мс
        private const val WAV_HEADER = 44
        private const val LEVEL_GAIN = 6.0
    }
}
