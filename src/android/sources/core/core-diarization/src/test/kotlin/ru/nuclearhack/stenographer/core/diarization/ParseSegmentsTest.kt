package ru.nuclearhack.stenographer.core.diarization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import ru.nuclearhack.stenographer.core.domain.engine.SpeakerSegment

class ParseSegmentsTest {

    @Test
    fun `native triples become zero-based segments in milliseconds`() {
        val raw = doubleArrayOf(
            3.2,
            6.0006,
            2.0,
            0.51,
            2.9,
            1.0,
        )

        assertEquals(
            listOf(
                SpeakerSegment(speaker = 0, startMs = 510, endMs = 2_900),
                SpeakerSegment(speaker = 1, startMs = 3_200, endMs = 6_001),
            ),
            parseSegments(raw),
        )
    }

    @Test
    fun `empty and degenerate segments are dropped`() {
        val raw = doubleArrayOf(
            1.0,
            1.0,
            1.0,
            2.0,
            3.0,
            0.0,
        )

        assertEquals(emptyList(), parseSegments(raw))
    }

    @Test
    fun `broken array is rejected`() {
        assertFailsWith<IllegalArgumentException> { parseSegments(doubleArrayOf(1.0, 2.0)) }
    }
}
