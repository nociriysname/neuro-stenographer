package ru.nuclearhack.stenographer.feature.transcript.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import ru.nuclearhack.stenographer.common.uikit.components.badge.StenoSpeakerBadge
import ru.nuclearhack.stenographer.common.uikit.components.badge.StenoSpeakerBadgeSize
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.format.formatTimestamp
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.tabular
import ru.nuclearhack.stenographer.core.model.Utterance

/**
 * Реплика в формате «Диалог»: пузырь цвета спикера. Первый спикер — справа («я»), остальные — слева с бейджем.
 * [current] — реплика с текущим совпадением поиска (рамка primary).
 */
@Composable
internal fun DialogMessage(
    utterance: Utterance,
    text: AnnotatedString,
    speakerIndex: Int,
    current: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = StenoTheme.colors
    val spacing = StenoTheme.spacing
    val own = speakerIndex == 0
    val speakerColor = colors.speaker(speakerIndex)
    val shapes = StenoTheme.shapes
    val shape = if (own) {
        shapes.lg.copy(bottomEnd = shapes.xs.bottomEnd)
    } else {
        shapes.lg.copy(bottomStart = shapes.xs.bottomStart)
    }
    val borderWidth: Dp = if (current) spacing.s1 / 2 else Dp.Hairline
    val borderColor = if (current) colors.primary else SpeakerTint.Border.over(colors.bg, speakerColor)
    val tint = if (own) SpeakerTint.BubbleOwn else SpeakerTint.Bubble

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (own) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (own) {
            Spacer(Modifier.width(spacing.s6))
        } else {
            StenoSpeakerBadge(speakerLabel(speakerIndex), speakerIndex, size = StenoSpeakerBadgeSize.Small)
            Spacer(Modifier.width(spacing.s2))
        }
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .clip(shape)
                .background(tint.over(colors.bg, speakerColor))
                .border(borderWidth, borderColor, shape)
                .padding(horizontal = spacing.s3 + spacing.s1 / 2, vertical = spacing.s2),
            verticalArrangement = Arrangement.spacedBy(spacing.s1),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                StenoText(speakerName(speakerIndex), style = StenoTheme.typography.caption, color = colors.text2)
                StenoText(
                    formatTimestamp(utterance.startMs),
                    style = StenoTheme.typography.caption.tabular(),
                    color = colors.primaryInk,
                )
            }
            StenoText(text)
        }
        if (own) Spacer(Modifier.width(spacing.s2)) else Spacer(Modifier.width(spacing.s6))
    }
}
