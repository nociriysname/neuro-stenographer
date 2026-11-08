package ru.nuclearhack.stenographer.feature.transcript.presentation.logic

import ru.nuclearhack.stenographer.core.domain.usecase.ProcessingStage

private const val MS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val MIN_FRACTION_FOR_ESTIMATE = 0.05f

/**
 * Общая доля готовности: этапы считаются равными по весу, поэтому цифра приблизительная
 * (реальные доли времени зависят от длины записи и устройства).
 */
fun overallFraction(stage: ProcessingStage, stageFraction: Float?): Float =
    (stage.ordinal + (stageFraction ?: 0f).coerceIn(0f, 1f)) / ProcessingStage.entries.size

/** Оценка остатка этапа по уже затраченному времени; `null`, пока данных мало или прогресса нет. */
fun estimateRemainingMs(elapsedMs: Long, fraction: Float?): Long? {
    if (fraction == null || fraction < MIN_FRACTION_FOR_ESTIMATE || fraction >= 1f) return null
    return (elapsedMs / fraction * (1f - fraction)).toLong()
}

/** «около 40 с» или «около 3 мин 40 с». */
fun formatRemaining(ms: Long): String {
    val seconds = ((ms + MS_PER_SECOND / 2) / MS_PER_SECOND).coerceAtLeast(1L)
    val minutes = seconds / SECONDS_PER_MINUTE
    return if (minutes == 0L) "около $seconds с" else "около $minutes мин ${seconds % SECONDS_PER_MINUTE} с"
}
