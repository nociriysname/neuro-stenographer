package ru.nuclearhack.stenographer

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.android.ActivityKey
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.core.domain.settings.SettingsRepository
import ru.nuclearhack.stenographer.core.domain.settings.ThemeMode
import ru.nuclearhack.stenographer.navigation.StenographerNavHost

@ActivityKey
@ContributesIntoMap(AppScope::class, binding<Activity>())
@Inject
class MainActivity(
    private val viewModelFactory: MetroViewModelFactory,
    private val settingsRepository: SettingsRepository,
) : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Стартовый экран выбирается один раз: после онбординга флаг меняется, но back stack уже другой.
        val showOnboarding = !settingsRepository.settings.value.onboardingCompleted
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle()
            val darkTheme = when (settings.themeMode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            // Иконки строки состояния и навигации — светлые на тёмной теме и наоборот, независимо от системной.
            LaunchedEffect(darkTheme) {
                val transparent = Color.TRANSPARENT
                val style = if (darkTheme) {
                    SystemBarStyle.dark(transparent)
                } else {
                    SystemBarStyle.light(transparent, transparent)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            StenoTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(LocalMetroViewModelFactory provides viewModelFactory) {
                    StenographerNavHost(showOnboarding = showOnboarding)
                }
            }
        }
    }
}
