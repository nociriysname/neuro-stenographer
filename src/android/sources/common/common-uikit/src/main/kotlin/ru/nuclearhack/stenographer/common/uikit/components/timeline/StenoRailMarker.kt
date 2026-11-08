package ru.nuclearhack.stenographer.common.uikit.components.timeline

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Метка на ленте [StenoTimelineRail]: найденное слово (accent) или эмоция (цвет маркера эмоции). */
@Immutable
data class StenoRailMarker(val atMs: Long, val color: Color)
