package ru.nuclearhack.stenographer.core.domain.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode { System, Light, Dark }

/** Пользовательские настройки. Хранятся на устройстве, никуда не отправляются. */
data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.System,
    /** GigaAM e2e_rnnt (с пунктуацией) или rnnt (без неё, чуть быстрее). */
    val punctuation: Boolean = true,
    val highlightEmotions: Boolean = true,
)

interface SettingsRepository {
    /** Текущие настройки; значение доступно сразу — по нему выбирается стартовый экран и тема. */
    val settings: StateFlow<AppSettings>

    suspend fun update(transform: (AppSettings) -> AppSettings)
}

/** Настройки по умолчанию без хранения — для инструментальных тестов движков. */
object DefaultSettings : SettingsRepository {
    override val settings: StateFlow<AppSettings> = MutableStateFlow(AppSettings())

    override suspend fun update(transform: (AppSettings) -> AppSettings) = Unit
}
