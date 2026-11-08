package ru.nuclearhack.stenographer.common.uikit.components.progress

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/**
 * Линейный прогресс. [progress] от 0 до 1; `null` — неопределённый: по треку бежит отрезок в треть длины
 * (при «убрать анимации» — статичная частичная полоса).
 */
@Composable
fun StenoLinearProgress(
    progress: Float?,
    modifier: Modifier = Modifier,
    height: StenoProgressHeight = StenoProgressHeight.Regular,
) {
    val colors = StenoTheme.colors
    val motion = StenoTheme.motion
    val animated by animateFloatAsState(progress?.coerceIn(0f, 1f) ?: 0f, motion.base(), label = "linearProgress")
    val phase = if (progress == null && !motion.reduced) {
        val transition = rememberInfiniteTransition(label = "indeterminate")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(motion.indeterminateMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "phase",
        )
        value
    } else {
        0.33f
    }
    val semanticsModifier = if (progress == null) {
        Modifier.semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }
    } else {
        Modifier.semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f) }
    }
    Canvas(modifier.fillMaxWidth().height(height.dp).then(semanticsModifier)) {
        val radius = CornerRadius(size.height / 2)
        drawRoundRect(colors.surface2, cornerRadius = radius)
        if (progress == null) {
            val segment = size.width * INDETERMINATE_SHARE
            val start = -segment + (size.width + segment) * phase
            clipRect {
                drawRoundRect(colors.primary, Offset(start, 0f), Size(segment, size.height), radius)
            }
        } else if (animated > 0f) {
            val barWidth = maxOf(size.height, size.width * animated)
            drawRoundRect(colors.primary, size = Size(barWidth, size.height), cornerRadius = radius)
        }
    }
}

private const val INDETERMINATE_SHARE = 0.34f

@Preview(name = "Light", showBackground = true)
@Composable
private fun StenoLinearProgressLightPreview() = StenoLinearProgressPreview(darkTheme = false)

@Preview(name = "Dark", showBackground = true)
@Composable
private fun StenoLinearProgressDarkPreview() = StenoLinearProgressPreview(darkTheme = true)

@Composable
private fun StenoLinearProgressPreview(darkTheme: Boolean) {
    StenoThemedSurface(darkTheme) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            StenoLinearProgress(progress = 0.62f)
            StenoLinearProgress(progress = 0.3f, height = StenoProgressHeight.Small)
            StenoLinearProgress(progress = null)
        }
    }
}
