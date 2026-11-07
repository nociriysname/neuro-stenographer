package ru.nuclearhack.stenographer.core.domain.processing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import ru.nuclearhack.stenographer.core.domain.engine.SpeakerSegment

class ChunkPlannerTest {

    private val planner = ChunkPlanner()
    private val config = ChunkPlanner.Config(maxChunkMs = 20_000, paddingMs = 200, maxSilenceInsideChunkMs = 1_000)

    @Test
    fun `short pauses between speakers stay inside one chunk`() {
        val segments = listOf(
            SpeakerSegment(speaker = 0, startMs = 1_000, endMs = 4_000),
            SpeakerSegment(speaker = 1, startMs = 4_500, endMs = 8_000),
        )

        val chunks = planner.plan(segments, audioDurationMs = 10_000, config = config)

        assertEquals(listOf(AudioChunk(800, 8_200)), chunks)
    }

    @Test
    fun `long silence splits chunks and is skipped`() {
        val segments = listOf(
            SpeakerSegment(speaker = 0, startMs = 0, endMs = 3_000),
            SpeakerSegment(speaker = 1, startMs = 10_000, endMs = 12_000),
        )

        val chunks = planner.plan(segments, audioDurationMs = 15_000, config = config)

        assertEquals(listOf(AudioChunk(0, 3_200), AudioChunk(9_800, 12_200)), chunks)
    }

    @Test
    fun `continuous speech longer than limit is split evenly`() {
        val segments = listOf(SpeakerSegment(speaker = 0, startMs = 0, endMs = 50_000))

        val chunks = planner.plan(segments, audioDurationMs = 50_000, config = config)

        assertEquals(3, chunks.size)
        assertTrue(chunks.all { it.durationMs <= config.maxChunkMs })
        assertEquals(0L, chunks.first().startMs)
        assertEquals(50_000L, chunks.last().endMs)
        chunks.zipWithNext().forEach { (a, b) -> assertEquals(a.endMs, b.startMs) }
    }

    @Test
    fun `chunks never exceed the limit after merging`() {
        val segments = (0 until 30).map { i ->
            SpeakerSegment(speaker = i % 2, startMs = i * 2_000L, endMs = i * 2_000L + 1_700)
        }

        val chunks = planner.plan(segments, audioDurationMs = 60_000, config = config)

        assertTrue(chunks.all { it.durationMs <= config.maxChunkMs }, "chunks: $chunks")
    }

    @Test
    fun `no speech found falls back to whole audio`() {
        val chunks = planner.plan(emptyList(), audioDurationMs = 30_000, config = config)

        assertEquals(listOf(AudioChunk(0, 15_000), AudioChunk(15_000, 30_000)), chunks)
    }
}
