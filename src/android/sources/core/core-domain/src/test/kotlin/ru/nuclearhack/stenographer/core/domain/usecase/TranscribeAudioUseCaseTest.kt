package ru.nuclearhack.stenographer.core.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import ru.nuclearhack.stenographer.core.domain.engine.Diarizer
import ru.nuclearhack.stenographer.core.domain.engine.EngineInfo
import ru.nuclearhack.stenographer.core.domain.engine.EngineKind
import ru.nuclearhack.stenographer.core.domain.engine.EngineUnavailableException
import ru.nuclearhack.stenographer.core.domain.engine.RecognizedWord
import ru.nuclearhack.stenographer.core.domain.engine.SpeakerSegment
import ru.nuclearhack.stenographer.core.domain.engine.SpeechRecognizer
import ru.nuclearhack.stenographer.core.domain.engine.TranscriptPostProcessor
import ru.nuclearhack.stenographer.core.domain.processing.ChunkPlanner
import ru.nuclearhack.stenographer.core.domain.processing.TranscriptAssembler
import ru.nuclearhack.stenographer.core.domain.repository.AudioRepository
import ru.nuclearhack.stenographer.core.model.AudioMetadata
import ru.nuclearhack.stenographer.core.model.AudioRef
import ru.nuclearhack.stenographer.core.model.PcmAudio
import ru.nuclearhack.stenographer.core.model.Transcript

class TranscribeAudioUseCaseTest {

    private val audio = PcmAudio(FloatArray(16_000 * 10)) // 10 секунд тишины

    private val audioRepository = object : AudioRepository {
        override suspend fun metadata(ref: AudioRef) = AudioMetadata("cats.wav", audio.durationMs)
        override suspend fun load(ref: AudioRef) = audio
    }

    private val diarizer = object : Diarizer {
        override val info = EngineInfo(EngineKind.Diarization, "fake", "test", isReady = true)
        override suspend fun diarize(audio: PcmAudio, onProgress: (Float) -> Unit): List<SpeakerSegment> {
            onProgress(1f)
            return listOf(SpeakerSegment(0, 1_000, 3_000), SpeakerSegment(1, 3_200, 6_000))
        }
    }

    /** Возвращает по слову на фрагмент и запоминает, какие сдвиги пришли. */
    private val recognizer = object : SpeechRecognizer {
        val offsets = mutableListOf<Long>()
        override val info = EngineInfo(EngineKind.SpeechRecognition, "fake", "test", isReady = true)
        override suspend fun recognize(audio: PcmAudio, offsetMs: Long): List<RecognizedWord> {
            offsets += offsetMs
            return listOf(
                RecognizedWord("Мяу", offsetMs + 300, offsetMs + 700),
                RecognizedWord("мур.", offsetMs + 2_700, offsetMs + 3_100),
            )
        }
    }

    private fun useCase(postProcessor: TranscriptPostProcessor) = TranscribeAudioUseCase(
        audioRepository = audioRepository,
        diarizer = diarizer,
        recognizer = recognizer,
        postProcessor = postProcessor,
        chunkPlanner = ChunkPlanner(),
        assembler = TranscriptAssembler(),
    )

    @Test
    fun `runs stages in order and returns assembled transcript`() = runTest {
        val upperCaser = object : TranscriptPostProcessor {
            override val info = EngineInfo(EngineKind.Llm, "fake", "test", isReady = true)
            override suspend fun process(transcript: Transcript) =
                transcript.copy(utterances = transcript.utterances.map { it.copy(text = it.text.uppercase()) })
        }

        val events = useCase(upperCaser)(AudioRef("content://cats")).toList()

        assertIs<TranscriptionEvent.Started>(events.first())
        val stages = events.filterIsInstance<TranscriptionEvent.Progress>().map { it.stage }.distinct()
        assertEquals(ProcessingStage.entries.toList(), stages)

        val completed = assertIs<TranscriptionEvent.Completed>(events.last())
        // Один фрагмент 800–6200 мс: слова на 1100 и 3500 мс → говорящие 0 и 1.
        assertEquals(listOf(800L), recognizer.offsets)
        assertEquals(listOf("МЯУ", "МУР."), completed.transcript.utterances.map { it.text })
        assertEquals(listOf("speaker_1", "speaker_2"), completed.transcript.utterances.map { it.speaker.value })
        assertEquals(10_000L, completed.timings.audioDurationMs)
        assertTrue(completed.warnings.isEmpty())
    }

    @Test
    fun `llm failure keeps the transcript and adds a warning`() = runTest {
        val broken = object : TranscriptPostProcessor {
            override val info = EngineInfo(EngineKind.Llm, "fake", "test", isReady = false)
            override suspend fun process(transcript: Transcript): Transcript =
                throw EngineUnavailableException(EngineKind.Llm, "нет модели")
        }

        val completed = assertIs<TranscriptionEvent.Completed>(useCase(broken)(AudioRef("x")).toList().last())

        assertEquals(listOf("Мяу", "мур."), completed.transcript.utterances.map { it.text })
        assertEquals(1, completed.warnings.size)
    }
}
