package ru.nuclearhack.stenographer.common.uikit.components.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
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
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.EmotionTone
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/** Метка эмоции реплики: цветной квадратик и название («Радость»). Эмоция всегда названа словом, не только цветом. */
@Composable
fun StenoEmotionTag(
    text: String,
    tone: EmotionTone,
    modifier: Modifier = Modifier,
) {
    val colors = StenoTheme.colors
    val emotion = colors.emotion(tone)
    Row(
        modifier = modifier
            .heightIn(min = 24.dp)
            .background(emotion.background, RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(emotion.mark, RoundedCornerShape(3.dp)))
        StenoText(text, style = StenoTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
    }
}

@Composable
private fun EmotionTagPreviewContent() {
    val names = listOf("Нейтрально", "Радость", "Раздражение", "Грусть", "Удивление")
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        EmotionTone.entries.forEach { StenoEmotionTag(names[it.ordinal], it) }
    }
}

@Preview(showBackground = true)
@Composable
private fun StenoEmotionTagLightPreview() {
    StenoThemedSurface(darkTheme = false) { EmotionTagPreviewContent() }
}

@Preview(showBackground = true)
@Composable
private fun StenoEmotionTagDarkPreview() {
    StenoThemedSurface(darkTheme = true) { EmotionTagPreviewContent() }
}
