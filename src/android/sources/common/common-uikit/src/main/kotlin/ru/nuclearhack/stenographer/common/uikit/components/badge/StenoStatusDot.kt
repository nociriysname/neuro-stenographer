package ru.nuclearhack.stenographer.common.uikit.components.badge

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/**
 * Квадратная точка-индикатор 8 dp (статус записи, «идёт обработка»). Декоративная: смысл всегда дублируется текстом.
 * При [pulsing] мигает, а если в системе отключены анимации — остаётся статичной.
 */
@Composable
fun StenoStatusDot(
    color: Color,
    modifier: Modifier = Modifier,
    pulsing: Boolean = false,
) {
    val motion = StenoTheme.motion
    var scale = 1f
    var alpha = 1f
    if (pulsing && !motion.reduced) {
        val transition = rememberInfiniteTransition(label = "dotPulse")
        val phase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(motion.pulseMillis / 2), RepeatMode.Reverse),
            label = "dotPhase",
        )
        scale = 1f - 0.2f * phase
        alpha = 1f - 0.65f * phase
    }
    Box(
        modifier = modifier
            .size(8.dp)
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
            .background(color, RoundedCornerShape(3.dp)),
    )
}

@Composable
private fun StatusDotPreviewContent() {
    val colors = StenoTheme.colors
    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StenoStatusDot(colors.success)
        StenoStatusDot(colors.rec, pulsing = true)
        StenoStatusDot(colors.warning)
        StenoStatusDot(colors.text3)
    }
}

@Preview(showBackground = true)
@Composable
private fun StenoStatusDotLightPreview() {
    StenoThemedSurface(darkTheme = false) { StatusDotPreviewContent() }
}

@Preview(showBackground = true)
@Composable
private fun StenoStatusDotDarkPreview() {
    StenoThemedSurface(darkTheme = true) { StatusDotPreviewContent() }
}
