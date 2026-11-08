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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/**
 * Волна громкости столбиками по центру. [levels] от 0 до 1; доля [playedFraction] окрашена, остальное — lineStrong.
 * В [StenoWaveformStyle.Recording] новый столбик справа, рисуются последние, что поместились;
 * в [StenoWaveformStyle.Primary] уровни сжимаются по ширине. Волна декоративна: скринридер её пропускает.
 */
@Composable
fun StenoWaveform(
    levels: List<Float>,
    modifier: Modifier = Modifier,
    playedFraction: Float = 1f,
    style: StenoWaveformStyle = StenoWaveformStyle.Primary,
    barWidth: Dp = 4.dp,
    gap: Dp = 3.dp,
) {
    val colors = StenoTheme.colors
    val active = if (style == StenoWaveformStyle.Recording) colors.rec else colors.primary
    Canvas(modifier.fillMaxWidth().height(96.dp).clearAndSetSemantics {}) {
        if (levels.isEmpty()) return@Canvas
        val bar = barWidth.toPx()
        val step = bar + gap.toPx()
        val fit = ((size.width + gap.toPx()) / step).toInt().coerceAtLeast(1)
        val shown = when {
            levels.size <= fit -> levels
            style == StenoWaveformStyle.Recording -> levels.takeLast(fit)
            else -> List(fit) { i ->
                val from = i * levels.size / fit
                val to = ((i + 1) * levels.size / fit).coerceAtLeast(from + 1)
                levels.subList(from, to).max()
            }
        }
        val total = shown.size * step - gap.toPx()
        val startX = if (style == StenoWaveformStyle.Recording) size.width - total else (size.width - total) / 2
        val played = playedFraction.coerceIn(0f, 1f) * shown.size
        shown.forEachIndexed { index, level ->
            val barHeight = (level.coerceIn(0f, 1f) * size.height).coerceAtLeast(size.height * MIN_BAR)
            drawRoundRect(
                color = if (index < played) active else colors.lineStrong,
                topLeft = Offset(startX + index * step, (size.height - barHeight) / 2),
                size = Size(bar, barHeight),
                cornerRadius = CornerRadius(bar / 2),
            )
        }
    }
}

private const val MIN_BAR = 0.08f

@Preview(name = "Light", showBackground = true)
@Composable
private fun StenoWaveformLightPreview() = StenoWaveformPreview(darkTheme = false)

@Preview(name = "Dark", showBackground = true)
@Composable
private fun StenoWaveformDarkPreview() = StenoWaveformPreview(darkTheme = true)

@Composable
private fun StenoWaveformPreview(darkTheme: Boolean) {
    StenoThemedSurface(darkTheme) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            StenoWaveform(previewWave(50, 1.3f), playedFraction = 0.4f)
            StenoWaveform(previewWave(30, 0.4f), style = StenoWaveformStyle.Recording)
        }
    }
}
