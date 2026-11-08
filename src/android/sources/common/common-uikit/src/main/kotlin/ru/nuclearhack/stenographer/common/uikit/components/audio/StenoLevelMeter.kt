package ru.nuclearhack.stenographer.common.uikit.components.audio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/** Индикатор уровня микрофона сегментами: пустые — surface3, заполненные — primary, верхние ~15% — warning. */
@Composable
fun StenoLevelMeter(
    level: Float,
    modifier: Modifier = Modifier,
    segments: Int = 24,
) {
    val colors = StenoTheme.colors
    val value = level.coerceIn(0f, 1f)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .semantics {
                contentDescription = "Уровень сигнала"
                progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f)
            },
    ) {
        val gap = 3.dp.toPx()
        val cell = (size.width - gap * (segments - 1)) / segments
        val lit = (value * segments).toInt()
        val hotFrom = (segments * HOT_FROM).toInt()
        repeat(segments) { i ->
            val color = when {
                i >= lit -> colors.surface3
                i >= hotFrom -> colors.warning
                else -> colors.primary
            }
            drawRoundRect(color, Offset(i * (cell + gap), 0f), Size(cell, size.height), CornerRadius(3.dp.toPx()))
        }
    }
}

private const val HOT_FROM = 0.85f

@Preview(name = "Light", showBackground = true)
@Composable
private fun StenoLevelMeterLightPreview() = StenoLevelMeterPreview(darkTheme = false)

@Preview(name = "Dark", showBackground = true)
@Composable
private fun StenoLevelMeterDarkPreview() = StenoLevelMeterPreview(darkTheme = true)

@Composable
private fun StenoLevelMeterPreview(darkTheme: Boolean) {
    StenoThemedSurface(darkTheme) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            StenoLevelMeter(level = 0.0f)
            StenoLevelMeter(level = 0.6f)
            StenoLevelMeter(level = 0.95f)
        }
    }
}
