package ru.nuclearhack.stenographer.core.data.export

import java.util.Locale
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.nuclearhack.stenographer.core.domain.repository.TranscriptFormat
import ru.nuclearhack.stenographer.core.model.SpeakerId
import ru.nuclearhack.stenographer.core.model.Transcript
import ru.nuclearhack.stenographer.core.model.Utterance

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
        .mapIndexed { index, it ->
            "${index + 1}\n${timestamp(it.startMs, ',')} --> ${timestamp(it.endMs, ',')}\n${it.speaker.value}: ${it.text}\n"
        }
        .joinToString(separator = "\n")

    /** `HH:MM:SS.mmm`; SRT требует запятую перед миллисекундами. */
    private fun timestamp(ms: Long, millisSeparator: Char): String {
        val hours = ms / 3_600_000
        val minutes = ms / 60_000 % 60
        val seconds = ms / 1_000 % 60
        val millis = ms % 1_000
        return String.format(Locale.ROOT, "%02d:%02d:%02d%c%03d", hours, minutes, seconds, millisSeparator, millis)
    }
}
