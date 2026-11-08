package ru.nuclearhack.stenographer.feature.transcript.presentation.logic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import ru.nuclearhack.stenographer.core.domain.repository.TranscriptFormat
import ru.nuclearhack.stenographer.core.domain.usecase.ProcessingStage
import ru.nuclearhack.stenographer.core.model.SpeakerId
import ru.nuclearhack.stenographer.core.model.Transcript
import ru.nuclearhack.stenographer.core.model.Utterance

class TranscriptLogicTest {
    private val utterances = listOf(
        Utterance(SpeakerId.of(2), 0, 1_000, "Бюджет и бюджет"),
        Utterance(SpeakerId.of(1), 2_000, 3_000, "Ничего"),
        Utterance(SpeakerId.of(2), 4_000, 5_000, "БЮДЖЕТ"),
    )

    @Test
    fun `search finds all occurrences ignoring case`() {
        val matches = findMatches(utterances, " бюджет ")
        assertEquals(listOf(0, 0, 2), matches.map { it.utteranceIndex })
        assertEquals(SearchMatch(0, 10, 16), matches[1].copy(start = 10, end = 16))
        assertEquals(emptyList(), findMatches(utterances, "  "))
    }

    @Test
    fun `speaker colors follow first appearance`() {
        val timeline = buildTimeline(Transcript(utterances), audioDurationMs = 10_000)
        assertEquals(listOf(0, 1, 0), timeline.speakerIndexByUtterance)
        assertEquals(2, timeline.speakerCount)
        assertEquals(10_000, timeline.durationMs)
        assertEquals(5_000, buildTimeline(Transcript(utterances), null).durationMs)
    }

    @Test
    fun `seek picks first unfinished utterance`() {
        assertEquals(1, utteranceIndexAt(utterances, 1_500))
        assertEquals(2, utteranceIndexAt(utterances, 99_000))
        assertEquals(-1, utteranceIndexAt(emptyList(), 0))
    }

    @Test
    fun `viewport reaches end of recording on last item`() {
        assertEquals(2_000L..3_000L, viewportRange(utterances, 1, 1, 10_000))
        assertEquals(2_000L..10_000L, viewportRange(utterances, 1, 2, 10_000))
    }

    @Test
    fun `progress and estimates`() {
        assertEquals(0f, overallFraction(ProcessingStage.Decoding, null))
        assertEquals(0.625f, overallFraction(ProcessingStage.Recognition, 0.5f))
        assertNull(estimateRemainingMs(1_000, 0.01f))
        assertNull(estimateRemainingMs(1_000, null))
        assertEquals(3_000L, estimateRemainingMs(1_000, 0.25f))
        assertEquals("около 40 с", formatRemaining(40_000))
        assertEquals("около 3 мин 40 с", formatRemaining(220_000))
    }

    @Test
    fun `export file name`() {
        assertEquals("zasedanie.srt", exportFileName("zasedanie.m4a", TranscriptFormat.Srt))
        assertEquals("transcript.json", exportFileName(null, TranscriptFormat.Json))
    }
}
