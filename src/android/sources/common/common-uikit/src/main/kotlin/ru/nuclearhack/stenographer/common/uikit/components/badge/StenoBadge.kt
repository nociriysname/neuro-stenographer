package ru.nuclearhack.stenographer.common.uikit.components.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoIcon
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/**
 * Статусная плашка («Готово», «Идёт запись», «Ошибка»). Смысл несёт текст, цвет его усиливает;
 * [pulsingDot] добавляет мигающую точку перед текстом, [icon] — иконку.
 */
@Composable
fun StenoBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: StenoBadgeTone = StenoBadgeTone.Neutral,
    icon: StenoIcons? = null,
    pulsingDot: Boolean = false,
) {
    val colors = StenoTheme.colors
    val background = when (tone) {
        StenoBadgeTone.Neutral -> colors.surface2
        StenoBadgeTone.Success -> colors.successSoft
        StenoBadgeTone.Warning -> colors.warningSoft
        StenoBadgeTone.Error -> colors.errorSoft
        StenoBadgeTone.Rec -> colors.recSoft
        StenoBadgeTone.Primary -> colors.primarySoft
    }
    val content = when (tone) {
        StenoBadgeTone.Neutral -> colors.text2
        StenoBadgeTone.Success -> colors.success
        StenoBadgeTone.Warning -> colors.warningInk
        StenoBadgeTone.Error -> colors.errorInk
        StenoBadgeTone.Rec -> colors.recInk
        StenoBadgeTone.Primary -> colors.primaryInk
    }
    Row(
        modifier = modifier
            .heightIn(min = 28.dp)
            .background(background, RoundedCornerShape(9.dp))
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (pulsingDot) StenoStatusDot(color = content, pulsing = true)
        if (icon != null) StenoIcon(icon, contentDescription = null, tint = content, size = 20.dp)
        StenoText(
            text = text,
            style = StenoTheme.typography.caption.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            color = content,
            maxLines = 1,
        )
    }
}

@Composable
private fun BadgePreviewContent() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StenoBadge("Черновик")
            StenoBadge("Готово", tone = StenoBadgeTone.Success, icon = StenoIcons.Check)
            StenoBadge("Внимание", tone = StenoBadgeTone.Warning, icon = StenoIcons.Warning)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StenoBadge("Ошибка", tone = StenoBadgeTone.Error)
            StenoBadge("Идёт запись", tone = StenoBadgeTone.Rec, pulsingDot = true)
            StenoBadge("Офлайн", tone = StenoBadgeTone.Primary, icon = StenoIcons.NoCloud)
        }
    }
}

@Preview(showBackground = true, widthDp = 420)
@Composable
private fun StenoBadgeLightPreview() {
    StenoThemedSurface(darkTheme = false) { BadgePreviewContent() }
}

@Preview(showBackground = true, widthDp = 420)
@Composable
private fun StenoBadgeDarkPreview() {
    StenoThemedSurface(darkTheme = true) { BadgePreviewContent() }
}
