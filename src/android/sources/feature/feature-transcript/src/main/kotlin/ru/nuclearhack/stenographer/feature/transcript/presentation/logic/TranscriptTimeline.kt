package ru.nuclearhack.stenographer.feature.transcript.presentation.logic

import ru.nuclearhack.stenographer.common.uikit.components.audio.StenoStripSegment
import ru.nuclearhack.stenographer.core.model.Transcript
import ru.nuclearhack.stenographer.core.model.Utterance

/** Данные ленты «Диалога»: сегменты по спикерам, номер цвета каждой реплики и длительность записи. */
data class TranscriptTimeline(
    val segments: List<StenoStripSegment>,
    val speakerIndexByUtterance: List<Int>,
    val speakerCount: Int,
    val durationMs: Long,
)

/** Цвет спикера = порядок первого появления. [audioDurationMs] — длительность записи, если известна. */
fun buildTimeline(transcript: Transcript, audioDurationMs: Long?): TranscriptTimeline {
    val order = transcript.speakers.withIndex().associate { it.value to it.index }
    val indices = transcript.utterances.map { order.getValue(it.speaker) }
    val lastEnd = transcript.utterances.maxOfOrNull { it.endMs } ?: 0L
    return TranscriptTimeline(
        segments = transcript.utterances.mapIndexed { i, u -> StenoStripSegment(u.startMs, u.endMs, indices[i]) },
        speakerIndexByUtterance = indices,
        speakerCount = order.size,
        durationMs = maxOf(audioDurationMs ?: 0L, lastEnd),
    )
}

/** Реплика, до которой нужно прокрутить список при перемотке на [ms]: первая, что ещё не закончилась. */
fun utteranceIndexAt(utterances: List<Utterance>, ms: Long): Int {
    if (utterances.isEmpty()) return -1
    val found = utterances.indexOfFirst { it.endMs >= ms }
    return if (found >= 0) found else utterances.lastIndex
}

/** Видимая часть записи для рамки на ленте по индексам первой и последней видимых реплик. */
fun viewportRange(utterances: List<Utterance>, first: Int, last: Int, durationMs: Long): LongRange {
    if (utterances.isEmpty()) return 0L..0L
    val from = utterances[first.coerceIn(0, utterances.lastIndex)]
    val to = utterances[last.coerceIn(0, utterances.lastIndex)]
    val reachesEnd = last >= utterances.lastIndex
    return from.startMs..(if (reachesEnd) durationMs else to.endMs)
}
