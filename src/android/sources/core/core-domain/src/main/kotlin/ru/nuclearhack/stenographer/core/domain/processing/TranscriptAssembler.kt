package ru.nuclearhack.stenographer.core.domain.processing

import dev.zacsweers.metro.Inject
import ru.nuclearhack.stenographer.core.domain.engine.RecognizedWord
import ru.nuclearhack.stenographer.core.domain.engine.SpeakerSegment
import ru.nuclearhack.stenographer.core.model.SpeakerId
import ru.nuclearhack.stenographer.core.model.Transcript
import ru.nuclearhack.stenographer.core.model.Utterance

/**
 * Сводит результат ASR (слова с таймкодами) и диаризации (кто когда говорил) в реплики.
 *
 * 1. Каждому слову назначается говорящий с максимальным перекрытием по времени. Слово в паузе между
 *    сегментами получает ближайший сегмент.
 * 2. Подряд идущие слова одного говорящего склеиваются в реплику. Новая реплика начинается при смене
 *    говорящего или при паузе длиннее [Config.maxPauseInsideUtteranceMs].
 * 3. Говорящие нумеруются `speaker_1`, `speaker_2`, … по первому появлению.
 *
 * Границы реплик берутся из слов, а не из сегментов диаризации: так таймкоды не захватывают тишину.
 */
@Inject
class TranscriptAssembler {

    data class Config(
        val maxPauseInsideUtteranceMs: Long = 1_500,
    )

    fun assemble(
        words: List<RecognizedWord>,
        segments: List<SpeakerSegment>,
        config: Config = Config(),
    ): Transcript {
        val sortedWords = words.filter { it.text.isNotBlank() }.sortedBy { it.startMs }
        if (sortedWords.isEmpty()) return Transcript.Empty
        val sortedSegments = segments.sortedBy { it.startMs }

        val speakerIds = LinkedHashMap<Int, SpeakerId>()
        val utterances = mutableListOf<Utterance>()
        val group = mutableListOf<RecognizedWord>()
        var groupSpeaker = 0

        fun flush() {
            if (group.isEmpty()) return
            val id = speakerIds.getOrPut(groupSpeaker) { SpeakerId.of(speakerIds.size + 1) }
            utterances += Utterance(
                speaker = id,
                startMs = group.first().startMs,
                endMs = group.maxOf { it.endMs },
                text = group.joinToString(" ") { it.text.trim() },
            )
            group.clear()
        }

        for (word in sortedWords) {
            val speaker = speakerFor(word, sortedSegments)
            val previous = group.lastOrNull()
            if (previous != null) {
                val pause = word.startMs - previous.endMs
                if (speaker != groupSpeaker || pause > config.maxPauseInsideUtteranceMs) flush()
            }
            if (group.isEmpty()) groupSpeaker = speaker
            group += word
        }
        flush()

        return Transcript(utterances)
    }

    private fun speakerFor(word: RecognizedWord, segments: List<SpeakerSegment>): Int {
        if (segments.isEmpty()) return 0

        val overlapBySpeaker = HashMap<Int, Long>()
        for (segment in segments) {
            if (segment.startMs >= word.endMs) break
            val overlap = minOf(word.endMs, segment.endMs) - maxOf(word.startMs, segment.startMs)
            if (overlap > 0) overlapBySpeaker.merge(segment.speaker, overlap, Long::plus)
        }
        overlapBySpeaker.maxByOrNull { it.value }?.let { return it.key }

        val middle = (word.startMs + word.endMs) / 2
        return segments.minBy { segment ->
            when {
                middle < segment.startMs -> segment.startMs - middle
                middle > segment.endMs -> middle - segment.endMs
                else -> 0
            }
        }.speaker
    }
}
