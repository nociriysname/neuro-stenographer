@file:Suppress("MagicNumber")

package ru.nuclearhack.stenographer.common.uikit.components.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/** Пример встречи 48:12 с тремя спикерами для превью: реплики и короткие паузы между ними. */
internal val PreviewMeetingSegments: List<StenoStripSegment> = run {
    val pattern = listOf(
        0 to 6, 1 to 5, 0 to 7, 2 to 4, 1 to 3, 0 to 8, 2 to 5, 1 to 6, 0 to 7, 2 to 4, 1 to 5, 0 to 6,
    )
    var cursor = 0L
    buildList {
        pattern.forEach { (speaker, units) ->
            val length = units * 18_000L
            add(StenoStripSegment(cursor, cursor + length, speaker))
            cursor += length
            add(StenoStripSegment(cursor, cursor + 6_000L, null))
            cursor += 6_000L
        }
    }
}

internal val PreviewMeetingDurationMs: Long = PreviewMeetingSegments.last().endMs

internal fun previewWave(count: Int, seed: Float): List<Float> = List(count) { i ->
    (0.25f + 0.65f * abs(sin(i * 0.37f * PI.toFloat() + seed)) * abs(sin(i * 0.11f + seed))).coerceIn(0f, 1f)
}

internal fun previewNoop() = Unit
