package ru.nuclearhack.stenographer.core.model

/** Идентификатор говорящего в формате ТЗ: `speaker_1`, `speaker_2`, … */
@JvmInline
value class SpeakerId(val value: String) {
    companion object {
        /** [ordinal] — порядковый номер говорящего с единицы, по первому появлению в записи. */
        fun of(ordinal: Int): SpeakerId {
            require(ordinal >= 1) { "Speaker ordinal starts from 1, got $ordinal" }
            return SpeakerId("speaker_$ordinal")
        }
    }
}

/** Одна реплика: кто, когда (мс от начала записи) и что сказал. */
data class Utterance(
    val speaker: SpeakerId,
    val startMs: Long,
    val endMs: Long,
    val text: String,
) {
    init {
        require(startMs >= 0) { "startMs must be >= 0, got $startMs" }
        require(endMs >= startMs) { "endMs ($endMs) must be >= startMs ($startMs)" }
    }

    val durationMs: Long get() = endMs - startMs
}

/** Структурированная расшифровка диалога. Реплики отсортированы по времени начала. */
data class Transcript(val utterances: List<Utterance>) {
    /** Говорящие в порядке первого появления. */
    val speakers: List<SpeakerId> get() = utterances.map { it.speaker }.distinct()

    companion object {
        val Empty = Transcript(emptyList())
    }
}
