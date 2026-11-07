package ru.nuclearhack.stenographer

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.android.ActivityKey
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import ru.nuclearhack.stenographer.common.uikit.theme.StenographerTheme
import ru.nuclearhack.stenographer.navigation.StenographerNavHost

@ActivityKey
@ContributesIntoMap(AppScope::class, binding<Activity>())
@Inject
class MainActivity(
    private val viewModelFactory: MetroViewModelFactory,
) : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            StenographerTheme {
                CompositionLocalProvider(LocalMetroViewModelFactory provides viewModelFactory) {
                    StenographerNavHost()
                }
            }
        }
    }
}
