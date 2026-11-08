package ru.nuclearhack.stenographer.feature.settings.presentation

import ru.nuclearhack.stenographer.core.domain.settings.ThemeMode
import ru.nuclearhack.stenographer.core.domain.usecase.ModelStorageStatus

/**
 * Состояние экрана настроек.
 *
 * [punctuatedAvailable] / [plainAvailable] — есть ли на устройстве веса соответствующего варианта GigaAM;
 * недоступный вариант выбрать нельзя. [checkProgress] — доля скопированных весов при проверке файлов
 * (`null` при [checking] — прогресс пока неизвестен).
 */
data class SettingsState(
    val punctuation: Boolean,
    val themeMode: ThemeMode,
    val highlightEmotions: Boolean,
    val punctuatedAvailable: Boolean,
    val plainAvailable: Boolean,
    val storage: ModelStorageStatus,
    val checking: Boolean = false,
    val checkProgress: Float? = null,
) {
    /**
     * Какой вариант распознавания реально используется: сохранённый выбор, а если его модели нет, то второй
     * (так поступает и движок). `null` — нет ни одной модели.
     */
    val effectivePunctuation: Boolean?
        get() = when {
            punctuation && punctuatedAvailable -> true
            !punctuation && plainAvailable -> false
            punctuatedAvailable -> true
            plainAvailable -> false
            else -> null
        }

    val installedModels: Int get() = storage.packs.count { it.installed }
    val totalModels: Int get() = storage.packs.size
}

sealed interface SettingsAction {
    data class SelectPunctuation(val punctuated: Boolean) : SettingsAction
    data class SelectTheme(val mode: ThemeMode) : SettingsAction
    data class SetHighlightEmotions(val enabled: Boolean) : SettingsAction
    data object CheckFiles : SettingsAction
}

sealed interface SettingsEffect {
    /** Короткое сообщение-тост; [success] — окрашивает его как успех, иначе как ошибку. */
    data class ShowMessage(val text: String, val success: Boolean) : SettingsEffect
}
