package ru.nuclearhack.stenographer.core.domain.usecase

import dev.zacsweers.metro.Inject
import ru.nuclearhack.stenographer.core.domain.engine.Diarizer
import ru.nuclearhack.stenographer.core.domain.engine.EngineInfo
import ru.nuclearhack.stenographer.core.domain.engine.ProtocolGenerator
import ru.nuclearhack.stenographer.core.domain.engine.SpeechRecognizer
import ru.nuclearhack.stenographer.core.domain.repository.TranscriptExporter
import ru.nuclearhack.stenographer.core.domain.repository.TranscriptFormat
import ru.nuclearhack.stenographer.core.model.DocumentRef
import ru.nuclearhack.stenographer.core.model.Transcript

/** Какие модели подключены и готовы — в порядке этапов: диаризация, распознавание, LLM (протокол встречи). */
@Inject
class GetEnginesStatusUseCase(
    private val diarizer: Diarizer,
    private val recognizer: SpeechRecognizer,
    private val protocolGenerator: ProtocolGenerator,
) {
    operator fun invoke(): List<EngineInfo> = listOf(diarizer.info, recognizer.info, protocolGenerator.info)
}

@Inject
class ExportTranscriptUseCase(
    private val exporter: TranscriptExporter,
) {
    suspend operator fun invoke(transcript: Transcript, format: TranscriptFormat, destination: DocumentRef) {
        exporter.export(transcript, format, destination)
    }
}
