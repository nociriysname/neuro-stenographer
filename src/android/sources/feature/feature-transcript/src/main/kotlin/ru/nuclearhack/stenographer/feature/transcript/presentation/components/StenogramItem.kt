package ru.nuclearhack.stenographer.feature.transcript.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import ru.nuclearhack.stenographer.common.uikit.components.badge.StenoSpeakerBadge
import ru.nuclearhack.stenographer.common.uikit.components.badge.StenoSpeakerBadgeSize
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.format.formatTimestamp
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.tabular
import ru.nuclearhack.stenographer.core.model.Utterance

/** Абзац формата «Стенограмма»: над текстом имя спикера и таймкоды; [current] — подложка primarySoft. */
@Composable
internal fun StenogramItem(
    utterance: Utterance,
    text: AnnotatedString,
    speakerIndex: Int,
    current: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = StenoTheme.colors
    val spacing = StenoTheme.spacing
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(StenoTheme.shapes.md)
            .background(if (current) colors.primarySoft else Color.Transparent)
            .padding(horizontal = spacing.s3, vertical = spacing.s2),
        verticalArrangement = Arrangement.spacedBy(spacing.s1),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
            StenoSpeakerBadge(speakerLabel(speakerIndex), speakerIndex, size = StenoSpeakerBadgeSize.Small)
            StenoText(speakerName(speakerIndex), style = StenoTheme.typography.label)
            StenoText(
                "${formatTimestamp(utterance.startMs)} – ${formatTimestamp(utterance.endMs)}",
                style = StenoTheme.typography.caption.tabular(),
                color = colors.primaryInk,
            )
        }
        StenoText(text, style = StenoTheme.typography.transcript)
    }
}
