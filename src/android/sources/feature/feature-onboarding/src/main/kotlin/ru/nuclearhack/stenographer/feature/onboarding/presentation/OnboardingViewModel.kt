package ru.nuclearhack.stenographer.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.nuclearhack.stenographer.common.uikit.udf.UdfViewModel
import ru.nuclearhack.stenographer.core.domain.settings.SettingsRepository
import ru.nuclearhack.stenographer.core.domain.usecase.GetModelStorageStatusUseCase
import ru.nuclearhack.stenographer.core.domain.usecase.ModelPackStatus
import ru.nuclearhack.stenographer.core.domain.usecase.ModelPreparation
import ru.nuclearhack.stenographer.core.domain.usecase.ModelStorageStatus
import ru.nuclearhack.stenographer.core.domain.usecase.PrepareModelsUseCase

/**
 * Онбординг: переключает шаги и на третьем запускает копирование моделей. Копирование идёт в viewModelScope
 * и не прерывается ни возвратом на прошлый шаг, ни «Пропустить»: недостающие модели всё равно доустановятся
 * при первом использовании.
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding<ViewModel>())
class OnboardingViewModel(
    getModelStorageStatus: GetModelStorageStatusUseCase,
    private val prepareModels: PrepareModelsUseCase,
    private val settings: SettingsRepository,
) : UdfViewModel<OnboardingState, OnboardingAction, OnboardingEffect>(initialState(getModelStorageStatus())) {

    private var copyJob: Job? = null
    private var completing = false

    override fun onAction(action: OnboardingAction) {
        when (action) {
            OnboardingAction.Next -> next()
            OnboardingAction.Back -> previous()
            OnboardingAction.Complete -> complete()
            OnboardingAction.RetryCopy -> startCopying(force = true)
        }
    }

    private fun next() {
        if (!currentState.canProceed) return
        val target = OnboardingStep.entries.getOrNull(currentState.step.ordinal + 1) ?: return
        updateState { copy(step = target) }
        if (target == OnboardingStep.Models) startCopying(force = false)
    }

    private fun previous() {
        val target = OnboardingStep.entries.getOrNull(currentState.step.ordinal - 1) ?: return
        updateState { copy(step = target) }
    }

    private fun startCopying(force: Boolean) {
        val models = currentState.models
        if (models == ModelsState.Done) return
        if (!force && models != ModelsState.Idle) return
        if (copyJob?.isActive == true) return
        updateState { copy(models = ModelsState.Copying(0, totalBytes)) }
        copyJob = viewModelScope.launch {
            try {
                prepareModels().collect(::onPreparation)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") failure: Exception) {
                // Причина не нужна экрану: он предлагает повторить, а при втором сбое модели доставятся лениво.
                updateState { copy(models = ModelsState.Failed) }
            }
        }
    }

    private fun onPreparation(preparation: ModelPreparation) {
        when (preparation) {
            is ModelPreparation.Copying -> updateState {
                val currentIndex = rows.indexOfFirst { it.kind == preparation.current }
                copy(
                    models = ModelsState.Copying(preparation.copiedBytes, preparation.totalBytes),
                    rows = rows.mapIndexed { index, row ->
                        when {
                            row.status == ModelRowStatus.Done -> row
                            index < currentIndex -> row.copy(status = ModelRowStatus.Done)
                            index == currentIndex -> row.copy(status = ModelRowStatus.Active)
                            else -> row
                        }
                    },
                )
            }
            is ModelPreparation.Done -> updateState {
                copy(models = ModelsState.Done, rows = preparation.status.toRows())
            }
            is ModelPreparation.Missing -> updateState {
                copy(models = ModelsState.Missing, rows = rows.markUnavailable(preparation.packs))
            }
        }
    }

    private fun complete() {
        if (completing) return
        completing = true
        viewModelScope.launch {
            settings.update { it.copy(onboardingCompleted = true) }
            sendEffect(OnboardingEffect.Finish)
        }
    }

    private companion object {
        fun initialState(status: ModelStorageStatus) = OnboardingState(
            models = if (status.allReady) ModelsState.Done else ModelsState.Idle,
            rows = status.toRows(),
        )

        fun ModelStorageStatus.toRows() = packs.map { it.toRow() }

        fun ModelPackStatus.toRow() = ModelRow(
            kind = kind,
            title = title,
            sizeBytes = sizeBytes,
            status = if (installed) ModelRowStatus.Done else ModelRowStatus.Pending,
        )

        fun List<ModelRow>.markUnavailable(missing: List<ModelPackStatus>): List<ModelRow> =
            map { row ->
                if (row.status != ModelRowStatus.Done && missing.any { it.kind == row.kind }) {
                    row.copy(status = ModelRowStatus.Unavailable)
                } else {
                    row
                }
            }
    }
}
