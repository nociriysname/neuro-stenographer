package ru.nuclearhack.stenographer.common.uikit.components.progress

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/** Заглушка загружаемого контента с бегущим бликом; при «убрать анимации» — ровная плашка surface2. */
@Composable
fun StenoSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = StenoTheme.shapes.xs,
) {
    val colors = StenoTheme.colors
    val motion = StenoTheme.motion
    val base = modifier.clip(shape)
    if (motion.reduced) {
        Box(base.background(colors.surface2))
        return
    }
    val transition = rememberInfiniteTransition(label = "skeleton")
    val shift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(motion.skeletonMillis, easing = LinearEasing), RepeatMode.Restart),
        label = "shift",
    )
    Box(
        base.drawWithCache {
            val width = size.width
            val brush = Brush.linearGradient(
                colors = listOf(colors.surface2, colors.surface1, colors.surface2),
                start = Offset(width * (shift * 2f - 1f), 0f),
                end = Offset(width * (shift * 2f), 0f),
            )
            onDrawBehind {
                drawRect(colors.surface2)
                drawRect(brush)
            }
        },
    )
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun StenoSkeletonLightPreview() = StenoSkeletonPreview(darkTheme = false)

@Preview(name = "Dark", showBackground = true)
@Composable
private fun StenoSkeletonDarkPreview() = StenoSkeletonPreview(darkTheme = true)

@Composable
private fun StenoSkeletonPreview(darkTheme: Boolean) {
    StenoThemedSurface(darkTheme) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StenoSkeleton(Modifier.fillMaxWidth().height(16.dp))
            StenoSkeleton(Modifier.fillMaxWidth(0.7f).height(16.dp))
            StenoSkeleton(Modifier.size(48.dp), shape = RoundedCornerShape(14.dp))
        }
    }
}
