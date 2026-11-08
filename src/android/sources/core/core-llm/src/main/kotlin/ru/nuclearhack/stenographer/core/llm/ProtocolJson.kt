package ru.nuclearhack.stenographer.core.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.nuclearhack.stenographer.core.model.MeetingProtocol
import ru.nuclearhack.stenographer.core.model.ProtocolTask

@Serializable
internal data class ProtocolDto(
    val title: String = "",
    val summary: String = "",
    val decisions: List<String> = emptyList(),
    val tasks: List<TaskDto> = emptyList(),
    @SerialName("key_points") val keyPoints: List<String> = emptyList(),
)

@Serializable
internal data class TaskDto(
    val task: String = "",
    val assignee: String? = null,
    val due: String? = null,
)

internal object ProtocolJson {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /** Ответ модели → протокол; null, если JSON не разобрался. */
    fun parse(answer: String): MeetingProtocol? {
        val start = answer.indexOf('{')
        val end = answer.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        val dto = runCatching { json.decodeFromString<ProtocolDto>(answer.substring(start, end + 1)) }.getOrNull()
            ?: return null
        return MeetingProtocol(
            title = dto.title.trim(),
            summary = dto.summary.trim(),
            decisions = dto.decisions.map(String::trim).filter(String::isNotEmpty),
            tasks = dto.tasks.filter { it.task.isNotBlank() }.map {
                ProtocolTask(it.task.trim(), it.assignee.clean(), it.due.clean())
            },
            keyPoints = dto.keyPoints.map(String::trim).filter(String::isNotEmpty),
        )
    }

    private fun String?.clean(): String? = this?.trim()?.takeUnless { it.isEmpty() || it.equals("null", true) }

    /** Протоколы частей длинной записи → один: списки подряд без повторов, содержание по частям. */
    fun merge(parts: List<MeetingProtocol>): MeetingProtocol = when (parts.size) {
        1 -> parts.single()
        else -> MeetingProtocol(
            title = parts.first().title,
            summary = parts.joinToString(" ") { it.summary }.trim(),
            decisions = parts.flatMap { it.decisions }.distinct(),
            tasks = parts.flatMap { it.tasks }.distinctBy { it.task.lowercase() },
            keyPoints = parts.flatMap { it.keyPoints }.distinct(),
        )
    }
}
