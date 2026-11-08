package ru.nuclearhack.stenographer.core.asr

import android.net.Uri
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.nuclearhack.stenographer.common.audio.AudioDecoder
import ru.nuclearhack.stenographer.common.models.ModelInstaller
import ru.nuclearhack.stenographer.core.domain.engine.RecognizedWord
import ru.nuclearhack.stenographer.core.domain.settings.DefaultSettings
import ru.nuclearhack.stenographer.core.model.PcmAudio
import java.io.File
import kotlin.time.Duration
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

/**
 * Прогон GigaAM-v3 на реальной записи на устройстве или эмуляторе. Запись кладут через adb:
 *
 * ```
 * ./gradlew :sources:core:core-asr:installDebugAndroidTest
 * adb push dialog.wav /sdcard/Android/data/ru.nuclearhack.stenographer.core.asr.test/files/dialog.wav
 * adb shell am instrument -w -e class ru.nuclearhack.stenographer.core.asr.GigaAmSpeechRecognizerDeviceTest \
 *     ru.nuclearhack.stenographer.core.asr.test/androidx.test.runner.AndroidJUnitRunner
 * ```
 *
 * Аргументы: `-e audio /путь` — другая запись (любой формат, который декодирует платформа),
 * `-e variant rnnt` — другой вариант модели. Его файлы подкладываются в `files/models/gigaam-v3-rnnt/`
 * тестового пакета, и каталогу нужен `adb shell chmod 777`: созданный adb, он недоступен приложению.
 * Запись распознаётся целиком, без нарезки. Слова пишутся рядом с ней в `<имя>.<вариант>.asr.json`.
 */
@RunWith(AndroidJUnit4::class)
class GigaAmSpeechRecognizerDeviceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val args = InstrumentationRegistry.getArguments()

    @Test
    fun recognizesRecordingFromDevice() = runBlocking {
        val file = args.getString("audio")?.let(::File) ?: File(context.getExternalFilesDir(null), "dialog.wav")
        assumeTrue("Нет записи ${file.path}", file.isFile)
        val audio = PcmAudio(AudioDecoder(context).decodeToMono(Uri.fromFile(file), PcmAudio.SAMPLE_RATE_16K))

        val installer = ModelInstaller(context)
        val recognizer = GigaAmSpeechRecognizer(installer, DefaultSettings)
        args.getString("variant")?.let { title ->
            recognizer.preferredVariant = GigaAmVariant.entries.single { it.title == title }
        }
        val variant = checkNotNull(recognizer.variant) { "Нет весов GigaAM-v3 на устройстве" }
        recognizer.preferredVariant?.let { assertEquals("Вариант не найден на устройстве", it, variant) }

        // Копирование весов из assets — разовая операция первого запуска, в RTF его не включаем.
        val (_, installTime) = measureTimedValue { installer.install(variant.assets) }
        var words = emptyList<RecognizedWord>()
        var decodeTime = Duration.ZERO
        val totalTime = measureTime {
            recognizer.withModelLoaded {
                decodeTime = measureTime { words = recognizer.recognize(audio, offsetMs = 0) }
            }
        }
        val loadTime = totalTime - decodeTime
        val rtf = decodeTime.inWholeMilliseconds.toDouble() / audio.durationMs
        val text = words.joinToString(" ") { it.text }

        Log.i(
            TAG,
            "variant=${variant.title} threads=${recognizer.numThreads} audio=${audio.durationMs} ms " +
                "install=${installTime.inWholeMilliseconds} ms load=${loadTime.inWholeMilliseconds} ms " +
                "decode=${decodeTime.inWholeMilliseconds} ms rtf=${"%.3f".format(rtf)} " +
                "peakRss=${peakRssMb()} MB words=${words.size}",
        )
        text.chunked(LOGCAT_CHUNK).forEach { Log.i(TAG, it) }

        val json = JSONObject()
            .put("variant", variant.title)
            .put("audio_ms", audio.durationMs)
            .put("load_ms", loadTime.inWholeMilliseconds)
            .put("decode_ms", decodeTime.inWholeMilliseconds)
            .put("rtf", rtf)
            .put("text", text)
            .put(
                "words",
                JSONArray(
                    words.map { JSONObject().put("text", it.text).put("start_ms", it.startMs).put("end_ms", it.endMs) },
                ),
            )
        File(file.parentFile, "${file.nameWithoutExtension}.${variant.title}.asr.json").writeText(json.toString(2))

        assertTrue("Модель не распознала ни слова", words.isNotEmpty())
        words.zipWithNext().forEach { (a, b) -> assertTrue("Слова не по порядку: $a, $b", a.startMs <= b.startMs) }
        words.forEach { word ->
            assertTrue("Слово за пределами записи: $word", word.startMs < word.endMs && word.endMs <= audio.durationMs)
        }
    }

    /** Пиковый резидентный объём процесса (VmHWM). */
    private fun peakRssMb(): Long = File("/proc/self/status").readLines()
        .firstOrNull { it.startsWith("VmHWM:") }
        ?.filter(Char::isDigit)?.toLongOrNull()?.div(1024) ?: -1

    private companion object {
        const val TAG = "StenoAsrTest"
        const val LOGCAT_CHUNK = 900
    }
}
