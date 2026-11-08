package ru.nuclearhack.stenographer.feature.transcript.presentation.components

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

/** Подпись и бейдж спикера: номер = порядок первого появления в записи. */
internal fun speakerName(speakerIndex: Int): String = "Спикер ${speakerIndex + 1}"

internal fun speakerLabel(speakerIndex: Int): String = (speakerIndex + 1).toString()
