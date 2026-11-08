package ru.nuclearhack.stenographer.feature.transcript.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ru.nuclearhack.stenographer.common.uikit.components.surface.StenoCard
import ru.nuclearhack.stenographer.common.uikit.components.surface.StenoCardStyle
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoIcon
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.tabular
import ru.nuclearhack.stenographer.core.domain.usecase.ProcessingTimings
import ru.nuclearhack.stenographer.feature.transcript.presentation.TranscriptPhase
import java.util.Locale

private const val MS_PER_SECOND = 1_000.0
private val RussianLocale: Locale = Locale.forLanguageTag("ru")

/** Секунды с одним знаком: «1,7 с». */
private fun seconds(ms: Long): String = String.format(RussianLocale, "%.1f с", ms / MS_PER_SECOND)

/**
 * Сводка обработки: говорящие, реплики, время и RTF. [detailed] добавляет время по этапам.
 * Предупреждения (например, пропущенная LLM-постобработка) показываются всегда.
 */
@Composable
internal fun ProcessingSummary(
    done: TranscriptPhase.Done,
    detailed: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = StenoTheme.colors
    val spacing = StenoTheme.spacing
    val timings = done.timings
    StenoCard(modifier = modifier.fillMaxWidth(), style = StenoCardStyle.Flat) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.s1)) {
            StenoText("Сводка обработки", style = StenoTheme.typography.label)
            StenoText(
                "Говорящих: ${done.transcript.speakers.size} · Реплик: ${done.transcript.utterances.size}",
                style = StenoTheme.typography.bodySmall,
                color = colors.text2,
            )
            StenoText(
                "Обработка ${seconds(timings.totalMs)} · RTF ${String.format(RussianLocale, "%.2f", timings.rtf)}",
                style = StenoTheme.typography.bodySmall.tabular(),
                color = colors.text2,
            )
            if (detailed) StageTimings(timings)
            done.warnings.forEach { warning ->
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.s2), verticalAlignment = Alignment.Top) {
                    StenoIcon(StenoIcons.Warning, contentDescription = null, tint = colors.warning, size = spacing.s5)
                    StenoText(warning, style = StenoTheme.typography.caption, color = colors.warningInk)
                }
            }
        }
    }
}

@Composable
private fun StageTimings(timings: ProcessingTimings) {
    val rows = listOf(
        "Декодирование" to timings.decodingMs,
        "Диаризация" to timings.diarizationMs,
        "Распознавание" to timings.recognitionMs,
        "Постобработка" to timings.postProcessingMs,
    )
    rows.forEach { (title, ms) ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StenoText(title, style = StenoTheme.typography.caption, color = StenoTheme.colors.text2)
            StenoText(seconds(ms), style = StenoTheme.typography.caption.tabular(), color = StenoTheme.colors.text2)
        }
    }
}
