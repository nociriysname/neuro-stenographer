package ru.nuclearhack.stenographer.feature.transcript.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** Насколько цвет спикера подмешивается к фону при окраске пузыря и его рамки. */
@Suppress("MagicNumber")
internal enum class SpeakerTint(val mix: Float) {
    Bubble(0.1f),
    BubbleOwn(0.16f),
    Border(0.3f),
}

internal fun SpeakerTint.over(background: Color, speaker: Color): Color = lerp(background, speaker, mix)

/** Имена, которые пользователь дал говорящим: индекс спикера (с нуля) → имя. */
internal val LocalSpeakerNames = staticCompositionLocalOf<Map<Int, String>> { emptyMap() }

/** Подпись спикера: имя, если его дали, иначе «Спикер N» (N — порядок первого появления в записи). */
@Composable
@ReadOnlyComposable
internal fun speakerName(speakerIndex: Int): String =
    LocalSpeakerNames.current[speakerIndex] ?: "Спикер ${speakerIndex + 1}"

/** Бейдж спикера: первая буква имени или номер. */
@Composable
@ReadOnlyComposable
internal fun speakerLabel(speakerIndex: Int): String =
    LocalSpeakerNames.current[speakerIndex]?.take(1)?.uppercase() ?: (speakerIndex + 1).toString()
