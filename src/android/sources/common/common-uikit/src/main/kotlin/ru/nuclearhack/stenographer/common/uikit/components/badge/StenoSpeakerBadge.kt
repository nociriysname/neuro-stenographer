package ru.nuclearhack.stenographer.common.uikit.components.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/**
 * Квадратный бейдж говорящего: цифра или инициал на цвете спикера [colorIndex] (до 8 цветов, дальше по кругу).
 * Цвет — не единственный признак: говорящего всегда различает и подпись [label].
 */
@Composable
fun StenoSpeakerBadge(
    label: String,
    colorIndex: Int,
    modifier: Modifier = Modifier,
    size: StenoSpeakerBadgeSize = StenoSpeakerBadgeSize.Medium,
) {
    val colors = StenoTheme.colors
    Box(
        modifier = modifier
            .size(size.side)
            .background(colors.speaker(colorIndex), RoundedCornerShape(size.radius)),
        contentAlignment = Alignment.Center,
    ) {
        StenoText(
            text = label,
            style = StenoTheme.typography.label.copy(fontSize = size.fontSize, fontWeight = FontWeight.Bold),
            color = colors.onSpeaker,
            maxLines = 1,
        )
    }
}

@Composable
private fun SpeakerBadgePreviewContent() {
    Row(
        Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(8) { StenoSpeakerBadge("${it + 1}", it, size = StenoSpeakerBadgeSize.Small) }
        StenoSpeakerBadge("И", 1)
        StenoSpeakerBadge("А", 3, size = StenoSpeakerBadgeSize.Large)
    }
}

@Preview(showBackground = true, widthDp = 420)
@Composable
private fun StenoSpeakerBadgeLightPreview() {
    StenoThemedSurface(darkTheme = false) { SpeakerBadgePreviewContent() }
}

@Preview(showBackground = true, widthDp = 420)
@Composable
private fun StenoSpeakerBadgeDarkPreview() {
    StenoThemedSurface(darkTheme = true) { SpeakerBadgePreviewContent() }
}
