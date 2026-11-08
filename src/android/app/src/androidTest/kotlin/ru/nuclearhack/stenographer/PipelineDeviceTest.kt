package ru.nuclearhack.stenographer

import android.net.Uri
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.nuclearhack.stenographer.common.audio.AudioDecoder
import ru.nuclearhack.stenographer.common.audio.AudioFileInfoReader
import ru.nuclearhack.stenographer.common.models.ModelInstaller
import ru.nuclearhack.stenographer.core.asr.GigaAmSpeechRecognizer
import ru.nuclearhack.stenographer.core.data.audio.AudioRepositoryImpl
import ru.nuclearhack.stenographer.core.data.export.TranscriptExporterImpl
import ru.nuclearhack.stenographer.core.diarization.NemotronDiarizer
import ru.nuclearhack.stenographer.core.domain.processing.ChunkPlanner
import ru.nuclearhack.stenographer.core.domain.processing.TranscriptAssembler
import ru.nuclearhack.stenographer.core.domain.repository.TranscriptFormat
import ru.nuclearhack.stenographer.core.domain.settings.DefaultSettings
import ru.nuclearhack.stenographer.core.domain.usecase.TranscribeAudioUseCase
import ru.nuclearhack.stenographer.core.domain.usecase.TranscriptionEvent
import ru.nuclearhack.stenographer.core.llm.LfmProtocolGenerator
import ru.nuclearhack.stenographer.core.llm.LlamaTranscriptPostProcessor
import ru.nuclearhack.stenographer.core.model.AudioRef
import ru.nuclearhack.stenographer.core.model.DocumentRef
import java.io.File
import kotlin.time.measureTimedValue

/**
 * Весь конвейер на устройстве с моделями из APK: декодирование → Nemotron → нарезка → GigaAM → сборка
 * реплик → LLM. Результат пишется в JSON формата организаторов — его можно сравнить с эталоном.
 *
 * ```
 * ./gradlew :app:installDebug :app:installDebugAndroidTest
 * adb push dialog.wav /sdcard/Android/data/ru.nuclearhack.stenographer/files/dialog.wav
 * adb shell am instrument -w -e class ru.nuclearhack.stenographer.PipelineDeviceTest \
 *     ru.nuclearhack.stenographer.test/androidx.test.runner.AndroidJUnitRunner
 * ```
 *
 * Запись — любой формат, который декодирует платформа; другой путь — `-e audio /путь`.
 * Расшифровка ложится рядом с записью в `<имя>.transcript.json`, тайминги — в logcat с тегом [TAG].
 */
@RunWith(AndroidJUnit4::class)
class PipelineDeviceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun transcribesRecordingEndToEnd() = runBlocking {
        val file = InstrumentationRegistry.getArguments().getString("audio")?.let(::File)
            ?: File(context.getExternalFilesDir(null), "dialog.wav")
        assumeTrue("Нет записи ${file.path}", file.isFile)

        // Тот же набор, что собирает граф Metro, только без UI.
        val installer = ModelInstaller(context)
        val transcribe = TranscribeAudioUseCase(
            audioRepository = AudioRepositoryImpl(AudioDecoder(context), AudioFileInfoReader(context)),
            diarizer = NemotronDiarizer(installer),
            recognizer = GigaAmSpeechRecognizer(installer, DefaultSettings),
            postProcessor = LlamaTranscriptPostProcessor(),
            chunkPlanner = ChunkPlanner(),
            assembler = TranscriptAssembler(),
        )

        val completed = transcribe(AudioRef(Uri.fromFile(file).toString()))
            .onEach { event ->
                if (event is TranscriptionEvent.Progress && event.fraction in setOf(null, 0f)) {
                    Log.i(TAG, "stage ${event.stage}")
                }
            }
            .filterIsInstance<TranscriptionEvent.Completed>()
            .first()

        with(completed.timings) {
            Log.i(
                TAG,
                "audio=$audioDurationMs ms decode=$decodingMs diar=$diarizationMs asr=$recognitionMs " +
                    "llm=$postProcessingMs total=$totalMs ms rtf=${"%.3f".format(rtf)}",
            )
        }
        completed.warnings.forEach { Log.w(TAG, it) }
        completed.transcript.utterances.forEach {
            Log.i(TAG, "${it.speaker.value} ${it.startMs}..${it.endMs}: ${it.text}")
        }

        val out = File(file.parentFile, "${file.nameWithoutExtension}.transcript.json")
        TranscriptExporterImpl(context).export(
            completed.transcript,
            TranscriptFormat.Json,
            DocumentRef(Uri.fromFile(out).toString()),
        )

        assertTrue("Пустая расшифровка", completed.transcript.utterances.isNotEmpty())

        // Протокол встречи (локальная LLM) — отдельный шаг поверх расшифровки, в RTF расшифровки не входит.
        if (InstrumentationRegistry.getArguments().getString("protocol") != "false") {
            val (protocol, took) = measureTimedValue {
                LfmProtocolGenerator(installer).generate(completed.transcript)
            }
            Log.i(TAG, "protocol ${took.inWholeMilliseconds} ms: $protocol")
            File(file.parentFile, "${file.nameWithoutExtension}.protocol.txt")
                .writeText("took_ms=${took.inWholeMilliseconds}\n$protocol\n")
            assertTrue("Пустой протокол", protocol.summary.isNotBlank())
        }
    }

    private companion object {
        const val TAG = "StenoPipelineTest"
    }
}
