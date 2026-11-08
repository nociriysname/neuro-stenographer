package ru.nuclearhack.stenographer.feature.transcript.presentation

import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.nuclearhack.stenographer.common.uikit.udf.UdfViewModel
import ru.nuclearhack.stenographer.core.domain.repository.TranscriptFormat
import ru.nuclearhack.stenographer.core.domain.usecase.ExportTranscriptUseCase
import ru.nuclearhack.stenographer.core.domain.usecase.ProcessingStage
import ru.nuclearhack.stenographer.core.domain.usecase.TranscribeAudioUseCase
import ru.nuclearhack.stenographer.core.domain.usecase.TranscriptionEvent
import ru.nuclearhack.stenographer.core.model.AudioRef
import ru.nuclearhack.stenographer.core.model.DocumentRef
import ru.nuclearhack.stenographer.feature.transcript.presentation.logic.estimateRemainingMs
import ru.nuclearhack.stenographer.feature.transcript.presentation.logic.exportFileName
import ru.nuclearhack.stenographer.feature.transcript.presentation.logic.findMatches
import kotlin.time.TimeSource

/**
 * Обработка одной записи и показ результата.
 *
 * Сейчас конвейер живёт в viewModelScope. Для релиза его стоит перенести в WorkManager с foreground
 * service (тип mediaProcessing), чтобы обработка не обрывалась при сворачивании приложения.
 */
@AssistedInject
class TranscriptViewModel(
    @Assisted private val audio: AudioRef,
    private val transcribeAudio: TranscribeAudioUseCase,
    private val exportTranscript: ExportTranscriptUseCase,
) : UdfViewModel<TranscriptState, TranscriptAction, TranscriptEffect>(TranscriptState()) {

    private var processing: Job? = null
    private var currentStage: ProcessingStage? = null
    private var stageStart = TimeSource.Monotonic.markNow()
    private val finishedStages = mutableMapOf<ProcessingStage, Long>()

    init {
        startProcessing()
    }

    override fun onAction(action: TranscriptAction) {
        when (action) {
            TranscriptAction.Retry -> startProcessing()
            TranscriptAction.Cancel -> cancelProcessing()
            is TranscriptAction.LayoutSelected -> updateState { copy(layout = action.layout) }
            TranscriptAction.SearchOpened -> updateState { copy(search = search.copy(open = true)) }
            TranscriptAction.SearchClosed -> updateState { copy(search = SearchState()) }
            is TranscriptAction.QueryChanged -> changeQuery(action.query)
            TranscriptAction.NextMatch -> moveMatch(+1)
            TranscriptAction.PreviousMatch -> moveMatch(-1)
            is TranscriptAction.SheetOpened -> updateState { copy(sheet = action.sheet) }
            TranscriptAction.SheetDismissed -> updateState { copy(sheet = null) }
            is TranscriptAction.ExportFormatSelected -> updateState { copy(exportFormat = action.format) }
            TranscriptAction.ExportConfirmed -> {
                updateState { copy(sheet = null) }
                requestExport(currentState.exportFormat)
            }
            is TranscriptAction.ExportDestinationChosen -> export(action.destination, action.format)
        }
    }

    private fun cancelProcessing() {
        processing?.cancel()
        updateState { copy(phase = TranscriptPhase.Failed("Расшифровка отменена")) }
    }

    private fun changeQuery(query: String) {
        val done = currentState.phase as? TranscriptPhase.Done
        val matches = done?.let { findMatches(it.transcript.utterances, query) }.orEmpty()
        updateState { copy(search = search.copy(query = query, matches = matches, current = 0)) }
    }

    private fun moveMatch(step: Int) {
        val count = currentState.search.matches.size
        if (count == 0) return
        updateState { copy(search = search.copy(current = (search.current + step).mod(count))) }
    }

    private fun startProcessing() {
        processing?.cancel()
        currentStage = null
        finishedStages.clear()
        processing = viewModelScope.launch {
            updateState { copy(phase = TranscriptPhase.Preparing, search = SearchState(), sheet = null) }
            try {
                transcribeAudio(audio).collect { event -> reduce(event) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateState { copy(phase = TranscriptPhase.Failed(e.userMessage())) }
            }
        }
    }

    private fun reduce(event: TranscriptionEvent) {
        when (event) {
            is TranscriptionEvent.Started -> updateState {
                copy(fileName = event.metadata.displayName, audioDurationMs = event.metadata.durationMs)
            }

            is TranscriptionEvent.Progress -> reduceProgress(event)

            is TranscriptionEvent.Completed -> updateState {
                copy(
                    audioDurationMs = event.timings.audioDurationMs,
                    phase = TranscriptPhase.Done(event.transcript, event.timings, event.warnings),
                )
            }
        }
    }

    private fun reduceProgress(event: TranscriptionEvent.Progress) {
        val previous = currentStage
        if (previous != event.stage) {
            if (previous != null) finishedStages[previous] = stageStart.elapsedNow().inWholeMilliseconds
            currentStage = event.stage
            stageStart = TimeSource.Monotonic.markNow()
        }
        val remaining = estimateRemainingMs(stageStart.elapsedNow().inWholeMilliseconds, event.fraction)
        updateState {
            copy(phase = TranscriptPhase.Processing(event.stage, event.fraction, finishedStages.toMap(), remaining))
        }
    }

    private fun requestExport(format: TranscriptFormat) {
        if (currentState.phase !is TranscriptPhase.Done) return
        sendEffect(TranscriptEffect.ChooseExportDestination(exportFileName(currentState.fileName, format), format))
    }

    private fun export(destination: String?, format: TranscriptFormat) {
        val done = currentState.phase as? TranscriptPhase.Done ?: return
        if (destination == null) return
        viewModelScope.launch {
            val message = try {
                exportTranscript(done.transcript, format, DocumentRef(destination))
                "Сохранено в ${format.extension.uppercase()}"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "Не удалось сохранить: ${e.userMessage()}"
            }
            sendEffect(TranscriptEffect.ShowMessage(message))
        }
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(@Assisted audio: AudioRef): TranscriptViewModel
    }
}

private fun Throwable.userMessage(): String = message ?: this::class.simpleName ?: "Неизвестная ошибка"
