@file:Suppress("MagicNumber")

package ru.nuclearhack.stenographer.feature.transcript.presentation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ru.nuclearhack.stenographer.common.uikit.components.overlay.rememberStenoToastHostState
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface
import ru.nuclearhack.stenographer.core.domain.repository.TranscriptFormat
import ru.nuclearhack.stenographer.core.domain.usecase.ProcessingStage
import ru.nuclearhack.stenographer.core.domain.usecase.ProcessingTimings
import ru.nuclearhack.stenographer.core.model.SpeakerId
import ru.nuclearhack.stenographer.core.model.Transcript
import ru.nuclearhack.stenographer.core.model.Utterance
import ru.nuclearhack.stenographer.feature.transcript.presentation.components.ExportSheetContent
import ru.nuclearhack.stenographer.feature.transcript.presentation.logic.findMatches

private const val PREVIEW_HEIGHT = 720
private const val MS_PER_MINUTE = 60_000L

private fun noOp() = Unit

@Suppress("UnusedParameter")
private fun noAction(action: TranscriptAction) = Unit

private fun line(speaker: Int, minute: Long, second: Long, text: String) = Utterance(
    SpeakerId.of(speaker),
    minute * MS_PER_MINUTE + second * 1_000L,
    minute * MS_PER_MINUTE + second * 1_000L + 14_000L,
    text,
)

private val previewTranscript = Transcript(
    listOf(
        line(1, 0, 4, "Коллеги, давайте вернёмся к бюджету на следующий квартал. У нас три вопроса."),
        line(2, 0, 21, "Опять бюджет? Мы же обсуждали его на прошлой неделе."),
        line(1, 0, 40, "Именно поэтому сегодня зафиксируем решение. Заказчик подтвердил финансирование."),
        line(3, 1, 2, "Подтвердил полностью? Это на двадцать процентов больше, чем мы рассчитывали."),
        line(1, 1, 25, "Да, но часть денег привязана к срокам. Бюджет надо согласовать до пятницы."),
        line(2, 1, 50, "Тогда я возьму на себя расчёты и пришлю таблицу сегодня вечером."),
        line(3, 2, 15, "Хорошо. А кто готовит презентацию для заказчика?"),
        line(1, 2, 40, "Я подготовлю, мне нужны только цифры от Игоря."),
    ),
)

private val previewTimings = ProcessingTimings(
    audioDurationMs = 3 * MS_PER_MINUTE,
    decodingMs = 120,
    diarizationMs = 4_200,
    recognitionMs = 9_800,
    postProcessingMs = 300,
    totalMs = 14_600,
)

private val previewDone = TranscriptPhase.Done(
    transcript = previewTranscript,
    timings = previewTimings,
    warnings = listOf("LLM-постобработка пропущена: модель не загружена"),
)

private val previewState = TranscriptState(
    fileName = "Планёрка отдела разработки.m4a",
    audioDurationMs = previewTimings.audioDurationMs,
    phase = previewDone,
)

private val previewSearch = SearchState(
    open = true,
    query = "бюджет",
    matches = findMatches(previewTranscript.utterances, "бюджет"),
    current = 1,
)

@Composable
private fun Preview(state: TranscriptState) {
    StenoThemedSurface {
        TranscriptContent(state, rememberStenoToastHostState(), onBack = ::noOp, onAction = ::noAction)
    }
}

@Preview(name = "Светлая", showBackground = true, heightDp = PREVIEW_HEIGHT)
@Preview(name = "Тёмная", showBackground = true, heightDp = PREVIEW_HEIGHT, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProcessingPreview() = Preview(
    previewState.copy(
        phase = TranscriptPhase.Processing(
            stage = ProcessingStage.Diarization,
            stageFraction = 0.64f,
            finishedMs = mapOf(ProcessingStage.Decoding to 8_000L),
            stageRemainingMs = 80_000L,
        ),
    ),
)

@Preview(name = "Светлая", showBackground = true, heightDp = PREVIEW_HEIGHT)
@Preview(name = "Тёмная", showBackground = true, heightDp = PREVIEW_HEIGHT, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DialogPreview() = Preview(previewState)

@Preview(name = "Светлая", showBackground = true, heightDp = PREVIEW_HEIGHT)
@Preview(name = "Тёмная", showBackground = true, heightDp = PREVIEW_HEIGHT, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DialogSearchPreview() = Preview(previewState.copy(search = previewSearch))

@Preview(name = "Светлая", showBackground = true, heightDp = PREVIEW_HEIGHT)
@Preview(name = "Тёмная", showBackground = true, heightDp = PREVIEW_HEIGHT, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StenogramPreview() = Preview(previewState.copy(layout = TranscriptLayout.Stenogram))

@Preview(name = "Светлая", showBackground = true, heightDp = PREVIEW_HEIGHT)
@Preview(name = "Тёмная", showBackground = true, heightDp = PREVIEW_HEIGHT, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StenogramSearchPreview() =
    Preview(previewState.copy(layout = TranscriptLayout.Stenogram, search = previewSearch))

@Preview(name = "Светлая", showBackground = true, heightDp = PREVIEW_HEIGHT)
@Preview(name = "Тёмная", showBackground = true, heightDp = PREVIEW_HEIGHT, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FailedPreview() =
    Preview(previewState.copy(phase = TranscriptPhase.Failed("Не хватает памяти для модели распознавания")))

@Preview(name = "Светлая", showBackground = true, heightDp = PREVIEW_HEIGHT)
@Preview(name = "Тёмная", showBackground = true, heightDp = PREVIEW_HEIGHT, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExportSheetPreview() {
    StenoThemedSurface {
        Column(
            modifier = Modifier.background(StenoTheme.colors.bg).padding(StenoTheme.spacing.screen),
            verticalArrangement = Arrangement.spacedBy(StenoTheme.spacing.s4),
        ) {
            ExportSheetContent(
                recordingName = previewState.fileName,
                selected = TranscriptFormat.Srt,
                onSelect = ::noFormat,
                onSave = ::noOp,
            )
        }
    }
}

@Suppress("UnusedParameter")
private fun noFormat(format: TranscriptFormat) = Unit
