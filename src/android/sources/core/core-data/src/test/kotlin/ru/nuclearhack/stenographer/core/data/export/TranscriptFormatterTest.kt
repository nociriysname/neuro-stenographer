package ru.nuclearhack.stenographer.core.data.export

import kotlin.test.Test
import kotlin.test.assertEquals
import ru.nuclearhack.stenographer.core.model.SpeakerId
import ru.nuclearhack.stenographer.core.model.Transcript
import ru.nuclearhack.stenographer.core.model.Utterance

class TranscriptFormatterTest {

    private val transcript = Transcript(
        listOf(
            Utterance(SpeakerId("speaker_1"), startMs = 1_250, endMs = 4_380, text = "Добрый день!"),
            Utterance(SpeakerId("speaker_2"), startMs = 3_725_010, endMs = 3_726_500, text = "Мяу."),
        ),
    )

    @Test
    fun `json matches the example from the task statement`() {
        val single = Transcript(transcript.utterances.take(1))

        val expected = """
            {
              "transcript": [
                {
                  "speaker_id": "speaker_1",
                  "start_ms": 1250,
                  "end_ms": 4380,
                  "text": "Добрый день!"
                }
              ]
            }
        """.trimIndent() + "\n"

        assertEquals(expected, TranscriptFormatter.toJson(single))
    }

    @Test
    fun `json round trip keeps everything`() {
        assertEquals(transcript, TranscriptFormatter.fromJson(TranscriptFormatter.toJson(transcript)))
    }

    @Test
    fun `empty transcript is still valid json`() {
        assertEquals("{\n  \"transcript\": []\n}\n", TranscriptFormatter.toJson(Transcript.Empty))
    }

    @Test
    fun `text export has readable timestamps`() {
        assertEquals(
            "[00:00:01.250 – 00:00:04.380] speaker_1: Добрый день!\n" +
                "[01:02:05.010 – 01:02:06.500] speaker_2: Мяу.\n",
            TranscriptFormatter.toText(transcript),
        )
    }

    @Test
    fun `srt export uses comma before milliseconds`() {
        assertEquals(
            "1\n00:00:01,250 --> 00:00:04,380\nspeaker_1: Добрый день!\n\n" +
                "2\n01:02:05,010 --> 01:02:06,500\nspeaker_2: Мяу.\n",
            TranscriptFormatter.toSrt(transcript),
        )
    }
}
