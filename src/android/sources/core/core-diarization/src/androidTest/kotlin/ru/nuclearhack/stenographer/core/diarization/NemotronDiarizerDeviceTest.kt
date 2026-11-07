package ru.nuclearhack.stenographer.core.diarization

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.time.measureTimedValue
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.nuclearhack.stenographer.common.models.ModelInstaller
import ru.nuclearhack.stenographer.core.model.PcmAudio

/**
 * Прогон Nemotron на реальной записи на устройстве или эмуляторе. Записи в git не хранятся, их кладут через adb:
 *
 * ```
 * ./gradlew :sources:core:core-diarization:installDebugAndroidTest
 * adb push dialog.wav /sdcard/Android/data/ru.nuclearhack.stenographer.core.diarization.test/files/dialog.wav
 * adb shell am instrument -w -e class ru.nuclearhack.stenographer.core.diarization.NemotronDiarizerDeviceTest \
 *     ru.nuclearhack.stenographer.core.diarization.test/androidx.test.runner.AndroidJUnitRunner
 * ```
 *
 * Нужен WAV 16 кГц моно 16 бит; другой путь — аргументом `-e audio /путь`. Без файла тест пропускается.
 * Сегменты пишутся рядом с записью в `<имя>.segments.json`, время и RTF — в logcat с тегом [TAG].
 */
@RunWith(AndroidJUnit4::class)
class NemotronDiarizerDeviceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun diarizesRecordingFromDevice() = runBlocking {
        val wav = InstrumentationRegistry.getArguments().getString("audio")?.let(::File)
            ?: File(context.getExternalFilesDir(null), "dialog.wav")
        assumeTrue("Нет записи ${wav.path}", wav.isFile)
        val audio = readWav16kMono(wav)

        val installer = ModelInstaller(context)
        // Копирование модели из assets — разовая операция первого запуска, в RTF его не включаем.
        val (_, installTime) = measureTimedValue { installer.install(NemotronDiarizer.MODEL) }

        val (segments, diarizeTime) = measureTimedValue { NemotronDiarizer(installer).diarize(audio) }
        val rtf = diarizeTime.inWholeMilliseconds.toDouble() / audio.durationMs
        Log.i(
            TAG,
            "audio=${audio.durationMs} ms install=${installTime.inWholeMilliseconds} ms " +
                "diarize=${diarizeTime.inWholeMilliseconds} ms rtf=${"%.3f".format(rtf)} " +
                "peakRss=${peakRssMb()} MB segments=${segments.size} " +
                "speakers=${segments.map { it.speaker }.distinct().sorted()}",
        )
        segments.forEach { Log.i(TAG, "speaker_${it.speaker + 1} ${it.startMs}..${it.endMs}") }

        val json = JSONObject()
            .put("audio_ms", audio.durationMs)
            .put("diarize_ms", diarizeTime.inWholeMilliseconds)
            .put("rtf", rtf)
            .put(
                "segments",
                JSONArray(
                    segments.map {
                        JSONObject().put("speaker", it.speaker).put("start_ms", it.startMs).put("end_ms", it.endMs)
                    },
                ),
            )
        File(wav.parentFile, wav.nameWithoutExtension + ".segments.json").writeText(json.toString(2))

        assertTrue("Модель не нашла речь", segments.isNotEmpty())
        segments.forEach { assertTrue("Сегмент за пределами записи: $it", it.endMs <= audio.durationMs + 100) }
    }

    /** Пиковый резидентный объём процесса (VmHWM): с учётом mmap-страниц модели. */
    private fun peakRssMb(): Long = File("/proc/self/status").readLines()
        .firstOrNull { it.startsWith("VmHWM:") }
        ?.filter(Char::isDigit)?.toLongOrNull()?.div(1024) ?: -1

    private fun readWav16kMono(file: File): PcmAudio {
        val buf = ByteBuffer.wrap(file.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
        val riff = buf.int
        buf.int // размер файла
        val wave = buf.int
        require(riff == RIFF && wave == WAVE) { "${file.name}: не RIFF/WAVE" }
        var channels = 0
        var sampleRate = 0
        var bits = 0
        while (buf.remaining() >= 8) {
            val id = buf.int
            val size = buf.int
            val next = buf.position() + size + (size and 1)
            when (id) {
                FMT -> {
                    buf.short // формат
                    channels = buf.short.toInt()
                    sampleRate = buf.int
                    buf.int
                    buf.short
                    bits = buf.short.toInt()
                }
                DATA -> {
                    assertEquals("${file.name}: нужен моно", 1, channels)
                    assertEquals("${file.name}: нужно 16 бит", 16, bits)
                    assertEquals("${file.name}: нужно 16 кГц", PcmAudio.SAMPLE_RATE_16K, sampleRate)
                    val samples = FloatArray(size / 2) { buf.short / 32768f }
                    return PcmAudio(samples, sampleRate)
                }
            }
            buf.position(next.coerceAtMost(buf.limit()))
        }
        error("${file.name}: нет блока data")
    }

    private companion object {
        const val TAG = "StenoDiarTest"
        const val RIFF = 0x46464952
        const val WAVE = 0x45564157
        const val FMT = 0x20746d66
        const val DATA = 0x61746164
    }
}
