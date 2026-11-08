package ru.nuclearhack.stenographer.common.uikit.components.badge

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Размер бейджа говорящего: сторона, радиус и кегль цифры. */
enum class StenoSpeakerBadgeSize(internal val side: Dp, internal val radius: Dp, internal val fontSize: TextUnit) {
    Small(24.dp, 8.dp, 12.sp),
    Medium(32.dp, 10.dp, 14.sp),
    Large(40.dp, 13.dp, 16.sp),
}
