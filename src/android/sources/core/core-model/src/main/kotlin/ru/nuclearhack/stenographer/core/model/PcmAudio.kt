package ru.nuclearhack.stenographer.core.model

/**
 * Моно PCM в float [-1; 1]. Внутренний формат всего конвейера: и GigaAM, и Nemotron ждут 16 кГц моно,
 * поэтому декодер приводит к нему любой входной файл один раз.
 */
class PcmAudio(
    val samples: FloatArray,
    val sampleRate: Int = SAMPLE_RATE_16K,
) {
    init {
        require(sampleRate > 0) { "sampleRate must be positive, got $sampleRate" }
    }

    val durationMs: Long get() = samples.size * 1000L / sampleRate

    /** Копия фрагмента [startMs; endMs). Границы обрезаются по длине записи. */
    fun slice(startMs: Long, endMs: Long): PcmAudio {
        val from = msToSample(startMs).coerceIn(0, samples.size)
        val to = msToSample(endMs).coerceIn(from, samples.size)
        return PcmAudio(samples.copyOfRange(from, to), sampleRate)
    }

    private fun msToSample(ms: Long): Int = (ms * sampleRate / MS_PER_SECOND).toInt()

    companion object {
        const val SAMPLE_RATE_16K = 16_000
        private const val MS_PER_SECOND = 1_000L
    }
}
