package ru.nuclearhack.stenographer.common.uikit.components.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoIconButton
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoIconButtonVariant
import ru.nuclearhack.stenographer.common.uikit.components.control.StenoToggleChip
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.format.formatTimestamp
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.tabular

/**
 * Мини-плеер экрана «Диалог»: шкала спикеров, время, а ниже — скорость (слева), «играть» (по центру)
 * и «пропускать тишину» (справа). Сверху линия и мягкая тень, снизу отступ под системную навигацию.
 */
@Suppress("LongParameterList")
@Composable
fun StenoMiniPlayer(
    segments: List<StenoStripSegment>,
    durationMs: Long,
    positionMs: Long,
    playing: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    speed: Float,
    onSpeedClick: () -> Unit,
    skipSilence: Boolean,
    onSkipSilenceToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = StenoTheme.colors
    val shadowColor = if (colors.isDark) Color(0x4D000000) else Color(0x1214202B)
    Column(
        modifier
            .fillMaxWidth()
            .dropShadow(RectangleShape, Shadow(radius = 24.dp, color = shadowColor, offset = DpOffset(0.dp, (-8).dp)))
            .background(colors.bg)
            .drawBehind { drawLine(colors.line, Offset.Zero, Offset(size.width, 0f), 1.dp.toPx()) }
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        StenoSpeakerStrip(segments, durationMs, positionMs = positionMs, onSeek = onSeek)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val style = StenoTheme.typography.caption.tabular()
            StenoText(formatTimestamp(positionMs), style = style, color = colors.text2)
            StenoText(formatTimestamp(durationMs), style = style, color = colors.text2)
        }
        Box(Modifier.fillMaxWidth().heightIn(min = 56.dp), contentAlignment = Alignment.Center) {
            StenoToggleChip(
                text = formatSpeed(speed),
                selected = speed != 1f,
                onClick = onSpeedClick,
                modifier = Modifier.align(Alignment.CenterStart).widthIn(min = 84.dp),
                icon = StenoIcons.Speed,
            )
            StenoPlayButton(playing = playing, onClick = onPlayPause)
            StenoIconButton(
                icon = StenoIcons.SkipSilence,
                contentDescription = if (skipSilence) "Пропускать тишину: включено" else "Пропускать тишину: выключено",
                onClick = onSkipSilenceToggle,
                modifier = Modifier.align(Alignment.CenterEnd),
                variant = if (skipSilence) StenoIconButtonVariant.Selected else StenoIconButtonVariant.Outlined,
            )
        }
    }
}

private fun formatSpeed(speed: Float): String =
    if (speed == speed.toInt().toFloat()) "${speed.toInt()}×" else "$speed×"

@Preview(name = "Light", showBackground = true)
@Composable
private fun StenoMiniPlayerLightPreview() = StenoMiniPlayerPreview(darkTheme = false)

@Preview(name = "Dark", showBackground = true)
@Composable
private fun StenoMiniPlayerDarkPreview() = StenoMiniPlayerPreview(darkTheme = true)

@Composable
private fun StenoMiniPlayerPreview(darkTheme: Boolean) {
    StenoThemedSurface(darkTheme) {
        StenoMiniPlayer(
            segments = PreviewMeetingSegments,
            durationMs = PreviewMeetingDurationMs,
            positionMs = PreviewMeetingDurationMs * 25 / 100,
            playing = true,
            onPlayPause = {},
            onSeek = {},
            speed = 1.5f,
            onSpeedClick = {},
            skipSilence = true,
            onSkipSilenceToggle = {},
        )
    }
}
