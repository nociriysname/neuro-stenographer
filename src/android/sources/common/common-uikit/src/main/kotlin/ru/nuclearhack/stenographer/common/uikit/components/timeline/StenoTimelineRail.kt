package ru.nuclearhack.stenographer.common.uikit.components.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import ru.nuclearhack.stenographer.common.uikit.components.audio.PreviewMeetingDurationMs
import ru.nuclearhack.stenographer.common.uikit.components.audio.PreviewMeetingSegments
import ru.nuclearhack.stenographer.common.uikit.components.audio.StenoStripSegment
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.format.formatTimestamp
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.tabular

/**
 * Вертикальная лента «Диалога»: вся запись сверху вниз, реплики спикеров столбиками (пересекающиеся — в двух дорожках),
 * рамка [viewportStartMs]..[viewportEndMs] показывает видимую часть списка, [markers] — метки поиска и эмоций.
 * Тап и перетаскивание перематывают; во время перетаскивания справа всплывает время.
 */
@Composable
fun StenoTimelineRail(
    segments: List<StenoStripSegment>,
    durationMs: Long,
    modifier: Modifier = Modifier,
    viewportStartMs: Long,
    viewportEndMs: Long,
    positionMs: Long?,
    markers: List<StenoRailMarker> = emptyList(),
    onSeek: (Long) -> Unit,
    width: Dp = 18.dp,
) {
    val colors = StenoTheme.colors
    val duration = durationMs.coerceAtLeast(1L)
    val currentSeek by rememberUpdatedState(onSeek)
    val lanes = remember(segments) { assignLanes(segments) }
    var dragY by remember { mutableStateOf<Float?>(null) }
    var dragMs by remember { mutableStateOf(0L) }
    val density = LocalDensity.current
    Box(
        modifier
            .width(width)
            .fillMaxHeight()
            .pointerInput(duration) {
                detectTapGestures { offset ->
                    currentSeek(railSeek(offset.y, size.height.toFloat(), duration))
                }
            }
            .pointerInput(duration) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        dragY = offset.y
                        dragMs = railSeek(offset.y, size.height.toFloat(), duration)
                        currentSeek(dragMs)
                    },
                    onDragEnd = { dragY = null },
                    onDragCancel = { dragY = null },
                ) { change, _ ->
                    change.consume()
                    val y = change.position.y.coerceIn(0f, size.height.toFloat())
                    dragY = y
                    dragMs = railSeek(y, size.height.toFloat(), duration)
                    currentSeek(dragMs)
                }
            }
            .semantics {
                contentDescription = "Лента диалога"
                val position = (positionMs ?: viewportStartMs).coerceIn(0L, duration)
                stateDescription = formatTimestamp(position)
                progressBarRangeInfo = ProgressBarRangeInfo(position.toFloat(), 0f..duration.toFloat())
                setProgress { value ->
                    currentSeek(value.toLong().coerceIn(0L, duration))
                    true
                }
            },
    ) {
        Canvas(Modifier.padding(vertical = 6.dp).fillMaxHeight().width(width)) {
            val h = size.height
            val w = size.width
            val radius = CornerRadius(9.dp.toPx())
            drawRoundRect(colors.surface1, size = size, cornerRadius = radius)
            drawRoundRect(colors.line, size = size, cornerRadius = radius, style = Stroke(1.dp.toPx()))
            val inset = 3.dp.toPx()
            val laneCount = if (lanes.maxLaneIndex > 0) 2 else 1
            val laneWidth = (w - inset * 2) / laneCount
            segments.forEachIndexed { i, segment ->
                val speaker = segment.speakerIndex ?: return@forEachIndexed
                val top = segment.startMs.toFloat() / duration * h
                val bottom = segment.endMs.toFloat() / duration * h
                drawRoundRect(
                    colors.speaker(speaker),
                    Offset(inset + lanes.laneOf[i] * laneWidth, top),
                    Size(laneWidth, maxOf(2.dp.toPx(), bottom - top)),
                    CornerRadius(2.dp.toPx()),
                )
            }
            markers.forEach { marker ->
                val y = marker.atMs.coerceIn(0L, duration).toFloat() / duration * h
                val tick = Size(w, 2.dp.toPx())
                drawRoundRect(marker.color, Offset(0f, y - tick.height / 2), tick, CornerRadius(1.dp.toPx()))
            }
            positionMs?.let {
                val y = it.coerceIn(0L, duration).toFloat() / duration * h
                val cursor = Size(w + 4.dp.toPx(), 3.dp.toPx())
                val topLeft = Offset(-2.dp.toPx(), y - cursor.height / 2)
                drawRoundRect(colors.text, topLeft, cursor, CornerRadius(1.5.dp.toPx()))
            }
            val vpTop = viewportStartMs.coerceIn(0L, duration).toFloat() / duration * h
            val vpBottom = viewportEndMs.coerceIn(0L, duration).toFloat() / duration * h
            val vpHeight = maxOf(30.dp.toPx(), vpBottom - vpTop).coerceAtMost(h)
            val top = vpTop.coerceAtMost(h - vpHeight)
            val bleed = 4.dp.toPx()
            val border = 2.dp.toPx()
            drawRoundRect(
                colors.primary.copy(alpha = 0.14f),
                Offset(-bleed, top),
                Size(w + bleed * 2, vpHeight),
                radius,
            )
            drawRoundRect(
                colors.primary,
                Offset(-bleed + border / 2, top + border / 2),
                Size(w + bleed * 2 - border, vpHeight - border),
                radius,
                style = Stroke(border),
            )
        }
        val y = dragY
        if (y != null) {
            val tooltipX = with(density) { (width + 12.dp).roundToPx() }
            val tooltipY = (y + with(density) { 6.dp.toPx() - 16.dp.toPx() }).toInt()
            Popup(offset = IntOffset(tooltipX, tooltipY)) {
                StenoText(
                    text = formatTimestamp(dragMs),
                    style = StenoTheme.typography.caption.tabular(),
                    color = colors.onToast,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.toast)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

private class RailLanes(val laneOf: IntArray, val maxLaneIndex: Int)

/** Два сегмента, перекрывающиеся во времени, уходят в разные дорожки (максимум две). */
private fun assignLanes(segments: List<StenoStripSegment>): RailLanes {
    val laneEnd = longArrayOf(Long.MIN_VALUE, Long.MIN_VALUE)
    val laneOf = IntArray(segments.size)
    var maxLane = 0
    segments.withIndex().sortedBy { it.value.startMs }.forEach { (i, segment) ->
        val lane = if (laneEnd[0] <= segment.startMs) 0 else 1
        laneEnd[lane] = segment.endMs
        laneOf[i] = lane
        if (lane > maxLane) maxLane = lane
    }
    return RailLanes(laneOf, maxLane)
}

private fun railSeek(y: Float, height: Float, durationMs: Long): Long =
    ((y / height.coerceAtLeast(1f)).coerceIn(0f, 1f) * durationMs).toLong()

@Preview(name = "Light", showBackground = true, heightDp = 480)
@Composable
private fun StenoTimelineRailLightPreview() = StenoTimelineRailPreview(darkTheme = false)

@Preview(name = "Dark", showBackground = true, heightDp = 480)
@Composable
private fun StenoTimelineRailDarkPreview() = StenoTimelineRailPreview(darkTheme = true)

@Composable
private fun StenoTimelineRailPreview(darkTheme: Boolean) {
    StenoThemedSurface(darkTheme) {
        val colors = StenoTheme.colors
        Box(Modifier.padding(24.dp).height(420.dp)) {
            StenoTimelineRail(
                segments = PreviewMeetingSegments,
                durationMs = PreviewMeetingDurationMs,
                viewportStartMs = PreviewMeetingDurationMs / 3,
                viewportEndMs = PreviewMeetingDurationMs / 3 + PreviewMeetingDurationMs / 10,
                positionMs = PreviewMeetingDurationMs / 3 + 20_000L,
                markers = listOf(
                    StenoRailMarker(PreviewMeetingDurationMs / 5, colors.accent),
                    StenoRailMarker(PreviewMeetingDurationMs * 3 / 5, colors.accent),
                    StenoRailMarker(PreviewMeetingDurationMs * 4 / 5, Color(0xFFBF2F78)),
                ),
                onSeek = {},
            )
        }
    }
}
