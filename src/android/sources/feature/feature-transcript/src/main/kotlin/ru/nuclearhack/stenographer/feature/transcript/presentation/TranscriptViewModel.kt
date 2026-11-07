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
import ru.nuclearhack.stenographer.core.domain.usecase.TranscribeAudioUseCase
import ru.nuclearhack.stenographer.core.domain.usecase.TranscriptionEvent
import ru.nuclearhack.stenographer.core.model.AudioRef
import ru.nuclearhack.stenographer.core.model.DocumentRef

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

    init {
        startProcessing()
    }

    override fun onAction(action: TranscriptAction) {
        when (action) {
            TranscriptAction.Retry -> startProcessing()
            is TranscriptAction.ExportRequested -> requestExport(action.format)
            is TranscriptAction.ExportDestinationChosen -> export(action.destination, action.format)
        }
    }

    private fun startProcessing() {
        processing?.cancel()
        processing = viewModelScope.launch {
            updateState { copy(phase = TranscriptPhase.Preparing) }
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

            is TranscriptionEvent.Progress -> updateState {
                copy(phase = TranscriptPhase.Processing(event.stage, event.fraction))
            }

            is TranscriptionEvent.Completed -> updateState {
                copy(
                    audioDurationMs = event.timings.audioDurationMs,
                    phase = TranscriptPhase.Done(event.transcript, event.timings, event.warnings),
                )
            }
        }
    }

    private fun requestExport(format: TranscriptFormat) {
        if (currentState.phase !is TranscriptPhase.Done) return
        val baseName = currentState.fileName
            ?.substringBeforeLast('.')
            ?.takeIf { it.isNotBlank() }
            ?: "transcript"
        sendEffect(TranscriptEffect.ChooseExportDestination("$baseName.${format.extension}", format))
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
