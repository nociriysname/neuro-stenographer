package ru.nuclearhack.stenographer.core.data.export

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.nuclearhack.stenographer.core.domain.repository.TranscriptFormat
import ru.nuclearhack.stenographer.core.model.SpeakerId
import ru.nuclearhack.stenographer.core.model.Transcript
import ru.nuclearhack.stenographer.core.model.Utterance
import java.util.Locale

/** DTO строго по ТЗ (стр. 15): имена полей и типы менять нельзя — по ним считают метрики. */
@Serializable
internal data class TranscriptDto(
    val transcript: List<UtteranceDto>,
)

@Serializable
internal data class UtteranceDto(
    @SerialName("speaker_id") val speakerId: String,
    @SerialName("start_ms") val startMs: Long,
    @SerialName("end_ms") val endMs: Long,
    val text: String,
)

internal object TranscriptFormatter {

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    fun format(transcript: Transcript, format: TranscriptFormat): String = when (format) {
        TranscriptFormat.Json -> toJson(transcript)
        TranscriptFormat.Text -> toText(transcript)
        TranscriptFormat.Srt -> toSrt(transcript)
    }

    fun toJson(transcript: Transcript): String =
        json.encodeToString(
            TranscriptDto.serializer(),
            TranscriptDto(
                transcript.utterances.map { UtteranceDto(it.speaker.value, it.startMs, it.endMs, it.text) },
            ),
        ) + "\n"

    fun fromJson(text: String): Transcript =
        Transcript(
            json.decodeFromString(TranscriptDto.serializer(), text).transcript.map {
                Utterance(SpeakerId(it.speakerId), it.startMs, it.endMs, it.text)
            },
        )

    fun toText(transcript: Transcript): String = transcript.utterances.joinToString(separator = "") {
        "[${timestamp(it.startMs, '.')} – ${timestamp(it.endMs, '.')}] ${it.speaker.value}: ${it.text}\n"
    }

    fun toSrt(transcript: Transcript): String = transcript.utterances
        .mapIndexed { index, utterance ->
            val start = timestamp(utterance.startMs, ',')
            val end = timestamp(utterance.endMs, ',')
            "${index + 1}\n$start --> $end\n${utterance.speaker.value}: ${utterance.text}\n"
        }
        .joinToString(separator = "\n")

    /** `HH:MM:SS.mmm`; SRT требует запятую перед миллисекундами. */
    private fun timestamp(ms: Long, millisSeparator: Char): String {
        val hours = ms / MS_PER_HOUR
        val minutes = ms / MS_PER_MINUTE % MINUTES_PER_HOUR
        val seconds = ms / MS_PER_SECOND % SECONDS_PER_MINUTE
        val millis = ms % MS_PER_SECOND
        return String.format(Locale.ROOT, "%02d:%02d:%02d%c%03d", hours, minutes, seconds, millisSeparator, millis)
    }

    private const val MS_PER_SECOND = 1_000L
    private const val MS_PER_MINUTE = 60_000L
    private const val MS_PER_HOUR = 3_600_000L
    private const val SECONDS_PER_MINUTE = 60L
    private const val MINUTES_PER_HOUR = 60L
}
