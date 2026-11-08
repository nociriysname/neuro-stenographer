package ru.nuclearhack.stenographer.core.model

/**
 * Протокол встречи, который составляет локальная LLM по готовой расшифровке.
 * В JSON для проверки (расшифровка) он не попадает: это отдельная функция поверх результата.
 */
data class MeetingProtocol(
    val title: String,
    val summary: String,
    val decisions: List<String>,
    val tasks: List<ProtocolTask>,
    val keyPoints: List<String>,
)

/**
 * Поручение. [assignee] — имя, если оно прозвучало, или `спикер_N`; null — исполнитель не назван.
 * [due] — срок так, как его назвали («до пятницы»), или null.
 */
data class ProtocolTask(
    val task: String,
    val assignee: String?,
    val due: String?,
)
