package ru.nuclearhack.stenographer.feature.home.presentation

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import ru.nuclearhack.stenographer.common.uikit.udf.UdfViewModel
import ru.nuclearhack.stenographer.core.domain.usecase.GetEnginesStatusUseCase
import ru.nuclearhack.stenographer.core.model.AudioRef

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding<ViewModel>())
class HomeViewModel(
    getEnginesStatus: GetEnginesStatusUseCase,
) : UdfViewModel<HomeState, HomeAction, HomeEffect>(HomeState(engines = getEnginesStatus())) {

    override fun onAction(action: HomeAction) {
        when (action) {
            is HomeAction.AudioPicked -> action.uri?.let { sendEffect(HomeEffect.OpenTranscript(AudioRef(it))) }
        }
    }
}
