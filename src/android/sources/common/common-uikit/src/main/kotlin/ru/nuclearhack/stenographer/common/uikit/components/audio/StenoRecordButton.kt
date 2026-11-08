package ru.nuclearhack.stenographer.common.uikit.components.audio

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoIcon
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface
import androidx.compose.ui.graphics.lerp as lerpColor

/**
 * Кнопка записи: ядро 80 dp (primary в ожидании, rec во время записи) и три «дышащих» ореола,
 * масштаб которых растёт с громкостью [level] (0..1). Общая область касания 96 dp.
 * При «убрать анимации» ореолы неподвижны.
 */
@Composable
fun StenoRecordButton(
    recording: Boolean,
    level: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = StenoTheme.colors
    val motion = StenoTheme.motion
    val coreColor = if (recording) colors.rec else colors.primary
    val smoothLevel by animateFloatAsState(level.coerceIn(0f, 1f), motion.fast(), label = "recordLevel")
    val breathing = recording && !motion.reduced
    val phases = if (breathing) {
        val transition = rememberInfiniteTransition(label = "recordHalo")
        listOf(HALO_FAST_MS, HALO_MID_MS, HALO_SLOW_MS).map { period ->
            transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(period, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "halo$period",
            )
        }
    } else {
        remember { List(HALO_COUNT) { mutableFloatStateOf(0.5f) } }
    }
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val description = if (recording) "Остановить запись" else "Начать запись"
    val coreShape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .size(96.dp)
            .clickable(
                interactionSource = source,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (recording) {
            Halos(phases = phases, level = { smoothLevel }, color = coreColor)
        }
        Box(
            Modifier
                .size(80.dp)
                .dropShadow(
                    coreShape,
                    Shadow(radius = 20.dp, color = coreColor.copy(alpha = 0.3f), offset = DpOffset(0.dp, 8.dp)),
                )
                .clip(coreShape)
                .background(if (pressed) lerpColor(coreColor, Color.Black, 0.12f) else coreColor),
            contentAlignment = Alignment.Center,
        ) {
            StenoIcon(
                icon = if (recording) StenoIcons.Stop else StenoIcons.Mic,
                contentDescription = null,
                tint = colors.onPrimary,
                size = 40.dp,
            )
        }
    }
}

@Composable
private fun Halos(phases: List<State<Float>>, level: () -> Float, color: Color) {
    Canvas(Modifier.size(96.dp)) {
        val core = 80.dp.toPx()
        val offset = Offset((size.width - core) / 2, (size.height - core) / 2)
        val lv = level()
        // Ореолы рисуются от дальнего к ближнему: (альфа, масштаб в покое, масштаб при пике, поворот, радиус пика).
        val specs = listOf(
            HaloSpec(0.08f, 1.06f, 1.18f + lv * 0.42f, -22f, 46f),
            HaloSpec(0.14f, 1.04f, 1.12f + lv * 0.30f, 14f, 38f),
            HaloSpec(0.22f, 1.02f, 1.07f + lv * 0.20f, 0f, 28f),
        )
        specs.forEachIndexed { index, spec ->
            val t = phases[index].value
            scale(lerp(spec.from, spec.to, t), pivot = offset + Offset(core / 2, core / 2)) {
                rotate(spec.angle * t, pivot = offset + Offset(core / 2, core / 2)) {
                    drawRoundRect(
                        color = color.copy(alpha = spec.alpha),
                        topLeft = offset,
                        size = Size(core, core),
                        cornerRadius = CornerRadius(lerp(28f, spec.radius, t).dp.toPx()),
                    )
                }
            }
        }
    }
}

private class HaloSpec(val alpha: Float, val from: Float, val to: Float, val angle: Float, val radius: Float)

private const val HALO_COUNT = 3
private const val HALO_FAST_MS = 850
private const val HALO_MID_MS = 1_350
private const val HALO_SLOW_MS = 2_100

@Preview(name = "Light", showBackground = true)
@Composable
private fun StenoRecordButtonLightPreview() = StenoRecordButtonPreview(darkTheme = false)

@Preview(name = "Dark", showBackground = true)
@Composable
private fun StenoRecordButtonDarkPreview() = StenoRecordButtonPreview(darkTheme = true)

@Composable
private fun StenoRecordButtonPreview(darkTheme: Boolean) {
    StenoThemedSurface(darkTheme) {
        Row(Modifier.padding(40.dp), horizontalArrangement = Arrangement.spacedBy(48.dp)) {
            StenoRecordButton(recording = false, level = 0f, onClick = ::previewNoop)
            StenoRecordButton(recording = true, level = 0.55f, onClick = ::previewNoop)
        }
    }
}
