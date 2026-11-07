package ru.nuclearhack.stenographer.core.domain.usecase

import dev.zacsweers.metro.Inject
import kotlin.time.TimeSource
import kotlin.time.measureTimedValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import ru.nuclearhack.stenographer.core.domain.engine.Diarizer
import ru.nuclearhack.stenographer.core.domain.engine.SpeechRecognizer
import ru.nuclearhack.stenographer.core.domain.engine.TranscriptPostProcessor
import ru.nuclearhack.stenographer.core.domain.processing.ChunkPlanner
import ru.nuclearhack.stenographer.core.domain.processing.TranscriptAssembler
import ru.nuclearhack.stenographer.core.domain.repository.AudioRepository
import ru.nuclearhack.stenographer.core.model.AudioRef

/**
 * Полный конвейер: декодирование → диаризация → нарезка → ASR → сборка реплик → LLM-постобработка.
 *
 * Диаризация идёт первой: её сегменты нужны и для нарезки (паузы), и для назначения слов говорящим.
 * Каждый этап замеряется — из этих замеров считается RTF для отчёта.
 */
@Inject
class TranscribeAudioUseCase(
    private val audioRepository: AudioRepository,
    private val diarizer: Diarizer,
    private val recognizer: SpeechRecognizer,
    private val postProcessor: TranscriptPostProcessor,
    private val chunkPlanner: ChunkPlanner,
    private val assembler: TranscriptAssembler,
) {
    operator fun invoke(audio: AudioRef): Flow<TranscriptionEvent> = channelFlow {
        val totalMark = TimeSource.Monotonic.markNow()
        send(TranscriptionEvent.Started(audioRepository.metadata(audio)))

        send(TranscriptionEvent.Progress(ProcessingStage.Decoding, fraction = null))
        val decoded = measureTimedValue { audioRepository.load(audio) }
        val pcm = decoded.value

        send(TranscriptionEvent.Progress(ProcessingStage.Diarization, fraction = 0f))
        val diarized = measureTimedValue {
            diarizer.diarize(pcm) { fraction ->
                trySend(TranscriptionEvent.Progress(ProcessingStage.Diarization, fraction))
            }
        }

        val chunks = chunkPlanner.plan(diarized.value, pcm.durationMs)
        val recognized = measureTimedValue {
            buildList {
                chunks.forEachIndexed { index, chunk ->
                    send(TranscriptionEvent.Progress(ProcessingStage.Recognition, index.toFloat() / chunks.size))
                    addAll(recognizer.recognize(pcm.slice(chunk.startMs, chunk.endMs), offsetMs = chunk.startMs))
                }
            }
        }

        val draft = assembler.assemble(recognized.value, diarized.value)

        send(TranscriptionEvent.Progress(ProcessingStage.PostProcessing, fraction = null))
        val warnings = mutableListOf<String>()
        val postProcessed = measureTimedValue {
            try {
                postProcessor.process(draft)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // LLM улучшает текст, но не должна блокировать результат: отдаём расшифровку без неё.
                warnings += "LLM-постобработка пропущена: ${e.message}"
                draft
            }
        }

        send(
            TranscriptionEvent.Completed(
                transcript = postProcessed.value,
                timings = ProcessingTimings(
                    audioDurationMs = pcm.durationMs,
                    decodingMs = decoded.duration.inWholeMilliseconds,
                    diarizationMs = diarized.duration.inWholeMilliseconds,
                    recognitionMs = recognized.duration.inWholeMilliseconds,
                    postProcessingMs = postProcessed.duration.inWholeMilliseconds,
                    totalMs = totalMark.elapsedNow().inWholeMilliseconds,
                ),
                warnings = warnings,
            ),
        )
    }
}
