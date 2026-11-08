package ru.nuclearhack.stenographer.feature.transcript.presentation.logic

import ru.nuclearhack.stenographer.core.model.MeetingProtocol
import ru.nuclearhack.stenographer.feature.transcript.presentation.TranscriptPhase
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Дата для подписи «Составлен на устройстве · 8 ноября». */
internal fun protocolDate(): String =
    LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru")))

/** `speaker_3` → 3: номер говорящего для цвета бейджа исполнителя. */
internal fun speakerNumbers(done: TranscriptPhase.Done): Map<String, Int> =
    done.transcript.speakers.associate { id -> id.value to id.value.substringAfterLast('_').toInt() }

/** Номер говорящего, если исполнитель — «спикер_N» / «speaker_N»; иначе null (прозвучало имя). */
internal fun assigneeSpeakerNumber(assignee: String): Int? =
    Regex("""^(спикер|speaker)[ _]?(\d+)$""", RegexOption.IGNORE_CASE).find(assignee.trim())
        ?.groupValues?.get(2)?.toIntOrNull()

/** «спикер_2» → «Спикер 2», имя — как есть. */
internal fun assigneeLabel(assignee: String): String =
    assigneeSpeakerNumber(assignee)?.let { "Спикер $it" } ?: assignee.trim()

/** Текст протокола для копирования и «Поделиться». */
internal fun formatProtocol(p: MeetingProtocol, recordingName: String?): String = buildString {
    appendLine(p.title.ifBlank { "Протокол встречи" })
    recordingName?.let { appendLine("Запись: $it") }
    appendLine()
    appendLine("Краткое содержание")
    appendLine(p.summary)
    if (p.decisions.isNotEmpty()) {
        appendLine()
        appendLine("Решения")
        p.decisions.forEach { appendLine("✓ $it") }
    }
    if (p.tasks.isNotEmpty()) {
        appendLine()
        appendLine("Задачи")
        p.tasks.forEach { t ->
            val meta = listOfNotNull(t.assignee?.let(::assigneeLabel), t.due?.let { "срок: $it" }).joinToString(", ")
            appendLine("☐ ${t.task}" + if (meta.isNotEmpty()) " — $meta" else "")
        }
    }
    if (p.keyPoints.isNotEmpty()) {
        appendLine()
        appendLine("Ключевые моменты")
        p.keyPoints.forEach { appendLine("• $it") }
    }
    appendLine()
    append("Составлен на устройстве локальной моделью, проверьте формулировки.")
}
