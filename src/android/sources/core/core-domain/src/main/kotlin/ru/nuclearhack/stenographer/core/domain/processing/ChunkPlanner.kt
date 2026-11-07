package ru.nuclearhack.stenographer.core.domain.processing

import dev.zacsweers.metro.Inject
import ru.nuclearhack.stenographer.core.domain.engine.SpeakerSegment

data class AudioChunk(val startMs: Long, val endMs: Long) {
    val durationMs: Long get() = endMs - startMs
}

/**
 * Режет запись на фрагменты для ASR.
 *
 * Диаризация заодно работает как VAD: объединение сегментов всех говорящих — это участки речи.
 * Границы фрагментов ставим в паузах, чтобы не разрезать слово, и пропускаем тишину, чтобы не тратить
 * на неё время энкодера. Речь без пауз длиннее [Config.maxChunkMs] режется принудительно на равные части.
 */
@Inject
class ChunkPlanner {

    data class Config(
        /** GigaAM обучалась на отрезках до ~25 с; 20 с оставляют запас. */
        val maxChunkMs: Long = 20_000,
        /** Поля вокруг речи: диаризация может «подрезать» тихие начала и концы слов. */
        val paddingMs: Long = 200,
        /** Паузы не длиннее этой остаются внутри одного фрагмента, длиннее — граница фрагмента. */
        val maxSilenceInsideChunkMs: Long = 1_000,
    )

    fun plan(
        segments: List<SpeakerSegment>,
        audioDurationMs: Long,
        config: Config = Config(),
    ): List<AudioChunk> {
        if (audioDurationMs <= 0) return emptyList()

        val speech = speechRegions(segments, audioDurationMs, config.paddingMs)
        // Диаризация не нашла речь — не верим ей на слово и прогоняем ASR по всей записи.
        if (speech.isEmpty()) return splitEvenly(AudioChunk(0, audioDurationMs), config.maxChunkMs)

        val packed = mutableListOf<AudioChunk>()
        var current = speech.first()
        for (region in speech.drop(1)) {
            val gap = region.startMs - current.endMs
            val merged = AudioChunk(current.startMs, region.endMs)
            if (gap <= config.maxSilenceInsideChunkMs && merged.durationMs <= config.maxChunkMs) {
                current = merged
            } else {
                packed += current
                current = region
            }
        }
        packed += current

        return packed.flatMap { splitEvenly(it, config.maxChunkMs) }
    }

    private fun speechRegions(
        segments: List<SpeakerSegment>,
        audioDurationMs: Long,
        paddingMs: Long,
    ): List<AudioChunk> {
        val padded = segments
            .map {
                AudioChunk(
                    startMs = (it.startMs - paddingMs).coerceAtLeast(0),
                    endMs = (it.endMs + paddingMs).coerceAtMost(audioDurationMs),
                )
            }
            .filter { it.endMs > it.startMs }
            .sortedBy { it.startMs }

        val merged = mutableListOf<AudioChunk>()
        for (region in padded) {
            val last = merged.lastOrNull()
            if (last != null && region.startMs <= last.endMs) {
                merged[merged.lastIndex] = AudioChunk(last.startMs, maxOf(last.endMs, region.endMs))
            } else {
                merged += region
            }
        }
        return merged
    }

    private fun splitEvenly(chunk: AudioChunk, maxChunkMs: Long): List<AudioChunk> {
        if (chunk.durationMs <= maxChunkMs) return listOf(chunk)
        val parts = ((chunk.durationMs + maxChunkMs - 1) / maxChunkMs).toInt()
        val step = chunk.durationMs / parts
        return (0 until parts).map { index ->
            AudioChunk(
                startMs = chunk.startMs + index * step,
                endMs = if (index == parts - 1) chunk.endMs else chunk.startMs + (index + 1) * step,
            )
        }
    }
}
