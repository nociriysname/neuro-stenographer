package ru.nuclearhack.stenographer.feature.transcript.presentation.logic

import ru.nuclearhack.stenographer.core.domain.repository.TranscriptFormat

/** Имя файла для «Сохранить как»: имя записи без расширения плюс расширение формата. */
fun exportFileName(recordingName: String?, format: TranscriptFormat): String {
    val base = recordingName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: "transcript"
    return "$base.${format.extension}"
}
