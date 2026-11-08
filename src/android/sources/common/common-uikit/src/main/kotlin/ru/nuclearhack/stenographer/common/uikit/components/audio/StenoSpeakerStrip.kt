package ru.nuclearhack.stenographer.common.uikit.components.audio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.format.formatTimestamp
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/**
 * Шкала спикеров — фирменный элемент приложения: вся запись одной полосой, цвет сегмента = цвет говорящего,
 * тишина — просвет трека. Реплики после [positionMs] приглушены. При [onSeek] по шкале можно тапать и тянуть.
 * Без [positionMs] и [onSeek] — компактная карточная версия (высота 8–10 dp).
 */
@Composable
fun StenoSpeakerStrip(
    segments: List<StenoStripSegment>,
    durationMs: Long,
    modifier: Modifier = Modifier,
    positionMs: Long? = null,
    onSeek: ((Long) -> Unit)? = null,
    height: Dp = 20.dp,
) {
    val colors = StenoTheme.colors
    val currentSeek by rememberUpdatedState(onSeek)
    val currentDuration by rememberUpdatedState(durationMs)
    val duration = durationMs.coerceAtLeast(1L)
    val verticalPad = if (positionMs != null) 3.dp else 0.dp
    val gesture = if (onSeek == null) {
        Modifier
    } else {
        Modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    currentSeek?.invoke(seekFor(offset.x, size.width.toFloat(), currentDuration))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    currentSeek?.invoke(seekFor(change.position.x, size.width.toFloat(), currentDuration))
                }
            }
    }
    val semanticsModifier = if (positionMs != null || onSeek != null) {
        Modifier.semantics {
            contentDescription = "Шкала записи по говорящим"
            val position = (positionMs ?: 0L).coerceIn(0L, duration)
            stateDescription = "${formatTimestamp(position)} из ${formatTimestamp(duration)}"
            progressBarRangeInfo = ProgressBarRangeInfo(position.toFloat(), 0f..duration.toFloat())
            if (onSeek != null) {
                setProgress { value ->
                    currentSeek?.invoke(value.toLong().coerceIn(0L, currentDuration))
                    true
                }
            }
        }
    } else {
        Modifier
    }
    Box(
        modifier
            .fillMaxWidth()
            .then(if (onSeek != null) Modifier.heightIn(min = 48.dp) else Modifier)
            .then(gesture)
            .then(semanticsModifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxWidth().height(height + verticalPad * 2)) {
            val stripTop = verticalPad.toPx()
            val stripHeight = height.toPx()
            val trackRadius = (height * 0.35f).toPx()
            val widthPx = size.width
            val trackPath = Path().apply {
                addRoundRect(
                    RoundRect(0f, stripTop, widthPx, stripTop + stripHeight, CornerRadius(trackRadius)),
                )
            }
            val gap = 0.5.dp.toPx()
            val minWidth = 1.dp.toPx()
            val playheadX = positionMs?.let { (it.coerceIn(0L, duration).toFloat() / duration) * widthPx }
            clipPath(trackPath) {
                drawRect(colors.surface2, Offset(0f, stripTop), Size(widthPx, stripHeight))
                fun drawSegments(alpha: Float) {
                    segments.forEach { segment ->
                        val speaker = segment.speakerIndex ?: return@forEach
                        val x0 = segment.startMs.toFloat() / duration * widthPx + gap
                        val x1 = segment.endMs.toFloat() / duration * widthPx - gap
                        drawRect(
                            colors.speaker(speaker).copy(alpha = alpha),
                            Offset(x0, stripTop),
                            Size(maxOf(minWidth, x1 - x0), stripHeight),
                        )
                    }
                }
                if (playheadX == null) {
                    drawSegments(1f)
                } else {
                    drawSegments(DIMMED_ALPHA)
                    clipRect(left = 0f, top = 0f, right = playheadX, bottom = size.height) { drawSegments(1f) }
                }
            }
            if (playheadX != null) {
                val x = playheadX.coerceIn(2.dp.toPx(), widthPx - 2.dp.toPx())
                val outline = 2.dp.toPx()
                val bar = 4.dp.toPx()
                drawRoundRect(
                    colors.bg,
                    Offset(x - bar / 2 - outline, 0f),
                    Size(bar + outline * 2, size.height),
                    CornerRadius(bar / 2 + outline),
                )
                drawRoundRect(colors.text, Offset(x - bar / 2, 0f), Size(bar, size.height), CornerRadius(bar / 2))
            }
        }
    }
}

private const val DIMMED_ALPHA = 0.4f

private fun seekFor(x: Float, width: Float, durationMs: Long): Long =
    ((x / width.coerceAtLeast(1f)).coerceIn(0f, 1f) * durationMs).toLong()

@Preview(name = "Light", showBackground = true)
@Composable
private fun StenoSpeakerStripLightPreview() = StenoSpeakerStripPreview(darkTheme = false)

@Preview(name = "Dark", showBackground = true)
@Composable
private fun StenoSpeakerStripDarkPreview() = StenoSpeakerStripPreview(darkTheme = true)

@Composable
private fun StenoSpeakerStripPreview(darkTheme: Boolean) {
    StenoThemedSurface(darkTheme) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            StenoSpeakerStrip(
                segments = PreviewMeetingSegments,
                durationMs = PreviewMeetingDurationMs,
                positionMs = PreviewMeetingDurationMs * 36 / 100,
                onSeek = {},
            )
            StenoSpeakerStrip(PreviewMeetingSegments, PreviewMeetingDurationMs, height = 10.dp)
            StenoSpeakerStrip(PreviewMeetingSegments, PreviewMeetingDurationMs, height = 8.dp)
        }
    }
}
