package ru.nuclearhack.stenographer.common.uikit.components.control

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.components.modifier.stenoFocusRing
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/**
 * Сегментированный переключатель на 2–4 варианта (режим, скорость, формат). Выбранный сегмент «приподнят»
 * над серым треком. Вместе с отступами трека высота сегмента даёт зону касания 48 dp.
 */
@Composable
fun StenoSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = StenoTheme.colors
    val itemShape = RoundedCornerShape(11.dp)
    Row(
        modifier = modifier
            .clip(StenoTheme.shapes.md)
            .background(colors.surface2)
            .padding(3.dp)
            .height(IntrinsicSize.Min)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            val itemBackground by animateColorAsState(
                targetValue = if (selected) colors.bg else colors.surface2,
                animationSpec = StenoTheme.motion.base(),
                label = "segmentBackground",
            )
            SegmentItem(
                text = option,
                selected = selected,
                background = itemBackground,
                onClick = { onSelect(index) },
                shape = itemShape,
            )
        }
    }
}

@Composable
private fun RowScope.SegmentItem(
    text: String,
    selected: Boolean,
    background: Color,
    onClick: () -> Unit,
    shape: RoundedCornerShape,
) {
    val colors = StenoTheme.colors
    Box(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 42.dp)
            .stenoFocusRing(shape)
            .then(if (selected) Modifier.dropShadow(shape, StenoTheme.elevation.card) else Modifier)
            .clip(shape)
            .background(background)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        StenoText(
            text = text,
            style = StenoTheme.typography.label,
            color = if (selected) colors.text else colors.text2,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SegmentedPreviewContent() {
    Box(Modifier.padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StenoSegmented(listOf("Диалог", "Текст"), 0, {})
            StenoSegmented(listOf("0,75×", "1×", "1,5×", "2×"), 2, {})
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun StenoSegmentedLightPreview() {
    StenoThemedSurface(darkTheme = false) { SegmentedPreviewContent() }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun StenoSegmentedDarkPreview() {
    StenoThemedSurface(darkTheme = true) { SegmentedPreviewContent() }
}
