package ru.nuclearhack.stenographer.core.domain.repository

import ru.nuclearhack.stenographer.core.model.AudioMetadata
import ru.nuclearhack.stenographer.core.model.AudioRef
import ru.nuclearhack.stenographer.core.model.DocumentRef
import ru.nuclearhack.stenographer.core.model.PcmAudio
import ru.nuclearhack.stenographer.core.model.Transcript

interface AudioRepository {
    suspend fun metadata(ref: AudioRef): AudioMetadata

    /** Декодирует запись любого поддерживаемого формата в моно PCM 16 кГц. */
    suspend fun load(ref: AudioRef): PcmAudio
}

enum class TranscriptFormat(val extension: String, val mimeType: String) {
    /** Формат из ТЗ: `{"transcript": [{"speaker_id", "start_ms", "end_ms", "text"}]}`. */
    Json("json", "application/json"),
    Text("txt", "text/plain"),
    Srt("srt", "application/x-subrip"),
}

interface TranscriptExporter {
    suspend fun export(transcript: Transcript, format: TranscriptFormat, destination: DocumentRef)
}
