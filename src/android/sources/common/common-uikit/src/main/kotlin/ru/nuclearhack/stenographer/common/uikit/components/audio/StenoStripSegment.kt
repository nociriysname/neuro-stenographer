package ru.nuclearhack.stenographer.common.uikit.components.audio

import androidx.compose.runtime.Immutable

/** Отрезок записи для шкалы спикеров: [speakerIndex] `null` — тишина или пауза между репликами. */
@Immutable
data class StenoStripSegment(val startMs: Long, val endMs: Long, val speakerIndex: Int?)
