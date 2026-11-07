package ru.nuclearhack.stenographer.feature.transcript.presentation

import ru.nuclearhack.stenographer.core.domain.repository.TranscriptFormat
import ru.nuclearhack.stenographer.core.domain.usecase.ProcessingStage
import ru.nuclearhack.stenographer.core.domain.usecase.ProcessingTimings
import ru.nuclearhack.stenographer.core.model.Transcript

data class TranscriptState(
    val fileName: String? = null,
    val audioDurationMs: Long? = null,
    val phase: TranscriptPhase = TranscriptPhase.Preparing,
)

sealed interface TranscriptPhase {
    data object Preparing : TranscriptPhase

    data class Processing(val stage: ProcessingStage, val progress: Float?) : TranscriptPhase

    data class Done(
        val transcript: Transcript,
        val timings: ProcessingTimings,
        val warnings: List<String>,
    ) : TranscriptPhase

    data class Failed(val message: String) : TranscriptPhase
}

sealed interface TranscriptAction {
    data object Retry : TranscriptAction

    data class ExportRequested(val format: TranscriptFormat) : TranscriptAction

    /** [destination] == null — пользователь закрыл диалог «Сохранить как». */
    data class ExportDestinationChosen(val destination: String?, val format: TranscriptFormat) : TranscriptAction
}

sealed interface TranscriptEffect {
    data class ChooseExportDestination(val suggestedName: String, val format: TranscriptFormat) : TranscriptEffect

    data class ShowMessage(val text: String) : TranscriptEffect
}
