package ru.nuclearhack.stenographer.core.domain.usecase

import ru.nuclearhack.stenographer.core.model.AudioMetadata
import ru.nuclearhack.stenographer.core.model.Transcript

enum class ProcessingStage { Decoding, Diarization, Recognition, PostProcessing }

data class ProcessingTimings(
    val audioDurationMs: Long,
    val decodingMs: Long,
    val diarizationMs: Long,
    val recognitionMs: Long,
    val postProcessingMs: Long,
    val totalMs: Long,
) {
    /** Real-time factor: время обработки / длительность записи. Меньше — лучше; по нему ставят 10 баллов. */
    val rtf: Double
        get() = if (audioDurationMs > 0) totalMs.toDouble() / audioDurationMs else 0.0
}

sealed interface TranscriptionEvent {
    data class Started(val metadata: AudioMetadata) : TranscriptionEvent

    /** [fraction] от 0 до 1, или null, если у этапа нет измеримого прогресса. */
    data class Progress(val stage: ProcessingStage, val fraction: Float?) : TranscriptionEvent

    data class Completed(
        val transcript: Transcript,
        val timings: ProcessingTimings,
        val warnings: List<String>,
    ) : TranscriptionEvent
}
