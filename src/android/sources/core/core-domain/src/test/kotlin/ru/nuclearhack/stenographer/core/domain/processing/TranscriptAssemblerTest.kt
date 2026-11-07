package ru.nuclearhack.stenographer.core.domain.processing

import kotlin.test.Test
import kotlin.test.assertEquals
import ru.nuclearhack.stenographer.core.domain.engine.RecognizedWord
import ru.nuclearhack.stenographer.core.domain.engine.SpeakerSegment
import ru.nuclearhack.stenographer.core.model.SpeakerId
import ru.nuclearhack.stenographer.core.model.Transcript
import ru.nuclearhack.stenographer.core.model.Utterance

class TranscriptAssemblerTest {

    private val assembler = TranscriptAssembler()

    @Test
    fun `words are grouped into utterances by speaker turns`() {
        val words = listOf(
            RecognizedWord("Добрый", 1_250, 1_700),
            RecognizedWord("день!", 1_750, 2_100),
            RecognizedWord("Мяу,", 2_600, 2_900),
            RecognizedWord("привет.", 2_950, 3_400),
        )
        val segments = listOf(
            SpeakerSegment(speaker = 3, startMs = 1_200, endMs = 2_200),
            SpeakerSegment(speaker = 5, startMs = 2_500, endMs = 3_500),
        )

        val transcript = assembler.assemble(words, segments)

        assertEquals(
            Transcript(
                listOf(
                    Utterance(SpeakerId("speaker_1"), 1_250, 2_100, "Добрый день!"),
                    Utterance(SpeakerId("speaker_2"), 2_600, 3_400, "Мяу, привет."),
                ),
            ),
            transcript,
        )
    }

    @Test
    fun `word goes to the speaker with the largest overlap`() {
        val words = listOf(
            RecognizedWord("Да", 100, 300),
            RecognizedWord("угу", 1_000, 1_400),
        )
        val segments = listOf(
            SpeakerSegment(speaker = 0, startMs = 0, endMs = 1_100),
            SpeakerSegment(speaker = 1, startMs = 1_050, endMs = 3_000),
        )

        val transcript = assembler.assemble(words, segments)

        // «угу»: 350 мс перекрытия со вторым говорящим против 100 мс с первым → отдельная реплика.
        assertEquals(
            listOf(SpeakerId("speaker_1") to "Да", SpeakerId("speaker_2") to "угу"),
            transcript.utterances.map { it.speaker to it.text },
        )
    }

    @Test
    fun `word in a gap gets the nearest segment`() {
        val words = listOf(
            RecognizedWord("раз", 100, 400),
            RecognizedWord("два", 2_900, 3_000),
        )
        val segments = listOf(
            SpeakerSegment(speaker = 0, startMs = 0, endMs = 500),
            SpeakerSegment(speaker = 1, startMs = 3_100, endMs = 4_000),
        )

        val transcript = assembler.assemble(words, segments)

        assertEquals(listOf("speaker_1", "speaker_2"), transcript.utterances.map { it.speaker.value })
    }

    @Test
    fun `long pause splits utterances of the same speaker`() {
        val words = listOf(
            RecognizedWord("Первая", 0, 500),
            RecognizedWord("мысль.", 550, 900),
            RecognizedWord("Вторая.", 4_000, 4_600),
        )
        val segments = listOf(SpeakerSegment(speaker = 0, startMs = 0, endMs = 5_000))

        val transcript = assembler.assemble(words, segments)

        assertEquals(listOf("Первая мысль.", "Вторая."), transcript.utterances.map { it.text })
        assertEquals(1, transcript.speakers.size)
    }

    @Test
    fun `speakers are numbered by first appearance`() {
        val words = listOf(
            RecognizedWord("a", 0, 100),
            RecognizedWord("b", 1_000, 1_100),
            RecognizedWord("c", 2_000, 2_100),
        )
        val segments = listOf(
            SpeakerSegment(speaker = 7, startMs = 0, endMs = 200),
            SpeakerSegment(speaker = 2, startMs = 900, endMs = 1_200),
            SpeakerSegment(speaker = 7, startMs = 1_900, endMs = 2_200),
        )

        val transcript = assembler.assemble(words, segments)

        assertEquals(listOf("speaker_1", "speaker_2", "speaker_1"), transcript.utterances.map { it.speaker.value })
    }

    @Test
    fun `no words gives empty transcript`() {
        assertEquals(Transcript.Empty, assembler.assemble(emptyList(), emptyList()))
    }
}
