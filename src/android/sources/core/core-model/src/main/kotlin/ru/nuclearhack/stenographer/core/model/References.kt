package ru.nuclearhack.stenographer.core.model

/**
 * Ссылка на аудиозапись. Для domain-слоя это непрозрачная строка; data-слой знает,
 * что внутри content:// URI из Storage Access Framework или путь к файлу записи.
 */
@JvmInline
value class AudioRef(val value: String)

/** Куда сохранить экспорт (URI, выбранный пользователем в системном диалоге). */
@JvmInline
value class DocumentRef(val value: String)

data class AudioMetadata(
    val displayName: String?,
    val durationMs: Long?,
)
