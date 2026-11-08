package ru.nuclearhack.stenographer.core.data.settings

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.withContext
import ru.nuclearhack.stenographer.core.domain.settings.AppSettings
import ru.nuclearhack.stenographer.core.domain.settings.SettingsRepository
import ru.nuclearhack.stenographer.core.domain.settings.ThemeMode

/**
 * Настройки в SharedPreferences: несколько флагов, читаются синхронно при старте (стартовый экран и тема
 * нужны до первого кадра), поэтому DataStore здесь не даёт выигрыша.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SettingsRepositoryImpl(context: Context) : SettingsRepository {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())

    override val settings: StateFlow<AppSettings> = state.asStateFlow()

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        val updated = state.updateAndGet(transform)
        withContext(Dispatchers.IO) { write(updated) }
    }

    private fun read() = AppSettings(
        onboardingCompleted = prefs.getBoolean(KEY_ONBOARDING, false),
        themeMode = prefs.getString(KEY_THEME, null)
            ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
            ?: ThemeMode.System,
        punctuation = prefs.getBoolean(KEY_PUNCTUATION, true),
        highlightEmotions = prefs.getBoolean(KEY_EMOTIONS, true),
    )

    private fun write(settings: AppSettings) {
        prefs.edit()
            .putBoolean(KEY_ONBOARDING, settings.onboardingCompleted)
            .putString(KEY_THEME, settings.themeMode.name)
            .putBoolean(KEY_PUNCTUATION, settings.punctuation)
            .putBoolean(KEY_EMOTIONS, settings.highlightEmotions)
            .commit()
    }

    private companion object {
        const val KEY_ONBOARDING = "onboarding_completed"
        const val KEY_THEME = "theme_mode"
        const val KEY_PUNCTUATION = "punctuation"
        const val KEY_EMOTIONS = "highlight_emotions"
    }
}
