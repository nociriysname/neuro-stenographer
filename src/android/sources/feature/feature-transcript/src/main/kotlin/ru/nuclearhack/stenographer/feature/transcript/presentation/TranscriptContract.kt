package ru.nuclearhack.stenographer.feature.transcript.presentation

import ru.nuclearhack.stenographer.core.domain.repository.TranscriptFormat
import ru.nuclearhack.stenographer.core.domain.usecase.ProcessingStage
import ru.nuclearhack.stenographer.core.domain.usecase.ProcessingTimings
import ru.nuclearhack.stenographer.core.model.MeetingProtocol
import ru.nuclearhack.stenographer.core.model.Transcript
import ru.nuclearhack.stenographer.feature.transcript.presentation.logic.SearchMatch

data class TranscriptState(
    val fileName: String? = null,
    val audioDurationMs: Long? = null,
    val phase: TranscriptPhase = TranscriptPhase.Preparing,
    val layout: TranscriptLayout = TranscriptLayout.Dialog,
    val search: SearchState = SearchState(),
    val sheet: TranscriptSheet? = null,
    val exportFormat: TranscriptFormat = TranscriptFormat.Json,
    /** Открыт экран «Протокол встречи» (S08); null — показан результат расшифровки. */
    val protocol: ProtocolUi? = null,
)

/** Протокол встречи от локальной LLM: составляется, готов или не получился. */
sealed interface ProtocolUi {
    data class Generating(val progress: Float) : ProtocolUi

    data class Ready(val protocol: MeetingProtocol, val createdAt: String, val speakerNumbers: Map<String, Int>) : ProtocolUi

    data class Failed(val message: String) : ProtocolUi
}

/** Формат показа результата: пузыри с лентой или плотная стенограмма. */
enum class TranscriptLayout(val title: String) {
    Dialog("Диалог"),
    Stenogram("Стенограмма"),
}

/** Открытый нижний лист. */
enum class TranscriptSheet { Export, Details }

/** Поиск по тексту: [current] — индекс текущего совпадения в [matches]. */
data class SearchState(
    val open: Boolean = false,
    val query: String = "",
    val matches: List<SearchMatch> = emptyList(),
    val current: Int = 0,
)

sealed interface TranscriptPhase {
    data object Preparing : TranscriptPhase

    /**
     * [stageFraction] — доля текущего этапа, `null` у этапов без измеримого прогресса.
     * [finishedMs] — длительность завершённых этапов, [stageRemainingMs] — оценка остатка текущего.
     */
    data class Processing(
        val stage: ProcessingStage,
        val stageFraction: Float?,
        val finishedMs: Map<ProcessingStage, Long> = emptyMap(),
        val stageRemainingMs: Long? = null,
    ) : TranscriptPhase

    data class Done(
        val transcript: Transcript,
        val timings: ProcessingTimings,
        val warnings: List<String>,
    ) : TranscriptPhase

    data class Failed(val message: String) : TranscriptPhase
}

sealed interface TranscriptAction {
    /** Открыть протокол: при первом открытии модель его составляет, дальше показывается готовый. */
    data object ProtocolOpened : TranscriptAction

    data object ProtocolClosed : TranscriptAction

    data object ProtocolRetry : TranscriptAction

    data object ProtocolCopy : TranscriptAction

    data object ProtocolShare : TranscriptAction

    data object Retry : TranscriptAction

    /** Отмена обработки: конвейер останавливается, экран закрывает вызывающий. */
    data object Cancel : TranscriptAction

    data class LayoutSelected(val layout: TranscriptLayout) : TranscriptAction

    data object SearchOpened : TranscriptAction

    data object SearchClosed : TranscriptAction

    data class QueryChanged(val query: String) : TranscriptAction

    data object NextMatch : TranscriptAction

    data object PreviousMatch : TranscriptAction

    data class SheetOpened(val sheet: TranscriptSheet) : TranscriptAction

    data object SheetDismissed : TranscriptAction

    data class ExportFormatSelected(val format: TranscriptFormat) : TranscriptAction

    /** «Сохранить в файл» в листе экспорта: запрашивает место сохранения для выбранного формата. */
    data object ExportConfirmed : TranscriptAction

    /** [destination] == null — пользователь закрыл диалог «Сохранить как». */
    data class ExportDestinationChosen(val destination: String?, val format: TranscriptFormat) : TranscriptAction
}

sealed interface TranscriptEffect {
    data class CopyText(val text: String) : TranscriptEffect

    data class ShareText(val title: String, val text: String) : TranscriptEffect

    data class ChooseExportDestination(val suggestedName: String, val format: TranscriptFormat) : TranscriptEffect

    data class ShowMessage(val text: String) : TranscriptEffect
}
