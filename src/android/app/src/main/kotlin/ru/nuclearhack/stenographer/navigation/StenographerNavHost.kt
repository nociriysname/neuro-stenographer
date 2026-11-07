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
import ru.nuclearhack.stenographer.feature.transcript.presentation.TranscriptScreen

@Serializable
data object HomeRoute : NavKey

@Serializable
data class TranscriptRoute(val audioUri: String) : NavKey

/**
 * Фичи не знают друг о друге: экран отдаёт наружу колбэки, а переходы связывает только :app.
 * ViewModelStore-декоратор даёт каждому экрану в back stack собственные ViewModel.
 */
@Composable
fun StenographerNavHost(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(HomeRoute)

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<HomeRoute> {
                HomeScreen(onOpenTranscript = { audio -> backStack.add(TranscriptRoute(audio.value)) })
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
