package ru.nuclearhack.stenographer.feature.transcript.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoButton
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoButtonSize
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoButtonVariant
import ru.nuclearhack.stenographer.common.uikit.components.progress.StenoProgressRing
import ru.nuclearhack.stenographer.common.uikit.components.progress.StenoStep
import ru.nuclearhack.stenographer.common.uikit.components.progress.StenoStepState
import ru.nuclearhack.stenographer.common.uikit.components.progress.StenoStepper
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoIcon
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.format.formatTimestamp
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.tabular
import ru.nuclearhack.stenographer.core.domain.usecase.ProcessingStage
import ru.nuclearhack.stenographer.feature.transcript.presentation.TranscriptPhase
import ru.nuclearhack.stenographer.feature.transcript.presentation.logic.formatRemaining
import ru.nuclearhack.stenographer.feature.transcript.presentation.logic.overallFraction

private const val PERCENT = 100

/** Экран обработки: кольцо общей готовности, оценка остатка этапа, этапы и отмена. [processing] `null` — подготовка. */
@Composable
internal fun ProcessingContent(
    processing: TranscriptPhase.Processing?,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = StenoTheme.colors
    val spacing = StenoTheme.spacing
    val fraction = processing?.let { overallFraction(it.stage, it.stageFraction) } ?: 0f
    val steps = remember(processing) { processingSteps(processing) }

    Column(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screen, vertical = spacing.s3),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            StenoProgressRing(progress = fraction, size = spacing.s16 * 2, strokeWidth = spacing.s2 + spacing.s1 / 2) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    StenoText(
                        "${(fraction * PERCENT).toInt()}%",
                        style = StenoTheme.typography.display.tabular(),
                    )
                    StenoText("готово", style = StenoTheme.typography.caption, color = colors.text2)
                }
            }
            StenoText(
                text = processing?.stageRemainingMs?.let { "До конца этапа ${formatRemaining(it)}" }
                    ?: processing?.stage?.title()
                    ?: "Подготовка…",
                style = StenoTheme.typography.subtitle.tabular(),
                textAlign = TextAlign.Center,
            )
            StenoStepper(steps, Modifier.padding(top = spacing.s4))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = spacing.screen, vertical = spacing.s3),
            verticalArrangement = Arrangement.spacedBy(spacing.s3),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StenoIcon(StenoIcons.Chip, contentDescription = null, tint = colors.text2, size = spacing.s5)
                StenoText(
                    "Все вычисления выполняются на устройстве",
                    style = StenoTheme.typography.caption,
                    color = colors.text2,
                )
            }
            StenoButton(
                text = "Отменить расшифровку",
                onClick = onCancel,
                variant = StenoButtonVariant.Secondary,
                size = StenoButtonSize.Large,
                icon = StenoIcons.Close,
                fillWidth = true,
            )
        }
    }
}

// Этап постобработки текста пустой (расшифровку не правим), поэтому в списке этапов его не показываем.
private fun processingSteps(processing: TranscriptPhase.Processing?): List<StenoStep> =
    ProcessingStage.entries.filter { it != ProcessingStage.PostProcessing }.map { stage ->
        val current = processing?.stage?.ordinal ?: -1
        when {
            stage.ordinal < current -> StenoStep(
                title = stage.stepTitle(),
                caption = processing?.finishedMs?.get(stage)?.let { "готово · ${formatTimestamp(it)}" } ?: "готово",
                state = StenoStepState.Done,
            )

            stage.ordinal == current && processing != null -> StenoStep(
                title = stage.stepTitle(),
                caption = activeCaption(processing),
                state = StenoStepState.Active,
            )

            else -> StenoStep(stage.stepTitle(), caption = null, state = StenoStepState.Pending)
        }
    }

private fun activeCaption(processing: TranscriptPhase.Processing): String = buildString {
    append("идёт")
    processing.stageFraction?.let { append(" · ${(it * PERCENT).toInt()}%") }
    processing.stageRemainingMs?.let { append(" · ≈ ${formatTimestamp(it)}") }
}

private fun ProcessingStage.stepTitle(): String = when (this) {
    ProcessingStage.Decoding -> "Декодирование"
    ProcessingStage.Diarization -> "Кто говорит"
    ProcessingStage.Recognition -> "Распознавание речи"
    ProcessingStage.PostProcessing -> "Постобработка"
}

private fun ProcessingStage.title(): String = when (this) {
    ProcessingStage.Decoding -> "Декодируем аудио"
    ProcessingStage.Diarization -> "Определяем голоса"
    ProcessingStage.Recognition -> "Распознаём речь"
    ProcessingStage.PostProcessing -> "Заключительная обработка"
}
