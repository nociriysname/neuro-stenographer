package ru.nuclearhack.stenographer.common.uikit.components.progress

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.tabular

/** Круговой прогресс: дуга primary по треку surface2, старт сверху, скруглённые концы; [content] — в центре. */
@Composable
fun StenoProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    strokeWidth: Dp = 8.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val colors = StenoTheme.colors
    val motion = StenoTheme.motion
    val value = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(value, motion.base(), label = "ringProgress")
    Box(
        modifier.size(size).semantics { progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f) },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(size)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2
            val arcSize = Size(this.size.width - stroke.width, this.size.height - stroke.width)
            val topLeft = Offset(inset, inset)
            drawArc(colors.surface2, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
            if (animated > 0f) {
                drawArc(
                    colors.primary,
                    -90f,
                    360f * animated,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke,
                )
            }
        }
        content()
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun StenoProgressRingLightPreview() = StenoProgressRingPreview(darkTheme = false)

@Preview(name = "Dark", showBackground = true)
@Composable
private fun StenoProgressRingDarkPreview() = StenoProgressRingPreview(darkTheme = true)

@Composable
private fun StenoProgressRingPreview(darkTheme: Boolean) {
    StenoThemedSurface(darkTheme) {
        StenoProgressRing(progress = 0.62f, modifier = Modifier.size(120.dp)) {
            StenoText("62%", style = StenoTheme.typography.subtitle.tabular(), textAlign = TextAlign.Center)
        }
    }
}
