package ru.nuclearhack.stenographer.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import ru.nuclearhack.stenographer.core.model.AudioRef
import ru.nuclearhack.stenographer.feature.home.presentation.HomeScreen
import ru.nuclearhack.stenographer.feature.onboarding.presentation.OnboardingScreen
import ru.nuclearhack.stenographer.feature.settings.presentation.SettingsScreen
import ru.nuclearhack.stenographer.feature.transcript.presentation.TranscriptScreen

@Serializable
data object OnboardingRoute : NavKey

@Serializable
data object HomeRoute : NavKey

@Serializable
data object SettingsRoute : NavKey

@Serializable
data class TranscriptRoute(val audioUri: String) : NavKey

/**
 * Фичи не знают друг о друге: экран отдаёт наружу колбэки, а переходы связывает только :app.
 * ViewModelStore-декоратор даёт каждому экрану в back stack собственные ViewModel.
 * [showOnboarding] — первый запуск: онбординг вместо главной.
 * [initialAudioUri] — запись, открытая из другого приложения: главная и поверх неё расшифровка.
 */
@Composable
fun StenographerNavHost(showOnboarding: Boolean, modifier: Modifier = Modifier, initialAudioUri: String? = null) {
    val backStack = when {
        showOnboarding -> rememberNavBackStack(OnboardingRoute)
        initialAudioUri != null -> rememberNavBackStack(HomeRoute, TranscriptRoute(initialAudioUri))
        else -> rememberNavBackStack(HomeRoute)
    }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<OnboardingRoute> {
                OnboardingScreen(
                    onFinished = {
                        backStack.add(HomeRoute)
                        backStack.remove(OnboardingRoute)
                    },
                )
            }
            entry<HomeRoute> {
                HomeScreen(
                    onOpenTranscript = { audio -> backStack.add(TranscriptRoute(audio.value)) },
                    onOpenSettings = { backStack.add(SettingsRoute) },
                )
            }
            entry<SettingsRoute> {
                SettingsScreen(onBack = { backStack.removeLastOrNull() })
            }
            entry<TranscriptRoute> { route ->
                TranscriptScreen(
                    audio = AudioRef(route.audioUri),
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
