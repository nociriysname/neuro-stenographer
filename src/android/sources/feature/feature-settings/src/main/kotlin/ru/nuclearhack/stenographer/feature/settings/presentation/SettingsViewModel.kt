package ru.nuclearhack.stenographer.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.launch
import ru.nuclearhack.stenographer.common.uikit.udf.UdfViewModel
import ru.nuclearhack.stenographer.core.domain.engine.SpeechRecognizer
import ru.nuclearhack.stenographer.core.domain.settings.AppSettings
import ru.nuclearhack.stenographer.core.domain.settings.SettingsRepository
import ru.nuclearhack.stenographer.core.domain.usecase.GetModelStorageStatusUseCase
import ru.nuclearhack.stenographer.core.domain.usecase.ModelPreparation
import ru.nuclearhack.stenographer.core.domain.usecase.PrepareModelsUseCase
import java.io.IOException

/**
 * Настройки: запись выбора в [SettingsRepository] (тема применяется сразу во всём приложении),
 * доступность вариантов GigaAM и проверка файлов моделей.
 */
@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding<ViewModel>())
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val recognizer: SpeechRecognizer,
    private val getStorageStatus: GetModelStorageStatusUseCase,
    private val prepareModels: PrepareModelsUseCase,
) : UdfViewModel<SettingsState, SettingsAction, SettingsEffect>(
    initialState = initialState(settingsRepository, recognizer, getStorageStatus),
) {

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                updateState {
                    copy(
                        punctuation = settings.punctuation,
                        themeMode = settings.themeMode,
                        highlightEmotions = settings.highlightEmotions,
                    )
                }
            }
        }
    }

    override fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SelectPunctuation -> selectPunctuation(action.punctuated)
            is SettingsAction.SelectTheme -> update { it.copy(themeMode = action.mode) }
            is SettingsAction.SetHighlightEmotions -> update { it.copy(highlightEmotions = action.enabled) }
            SettingsAction.CheckFiles -> checkFiles()
        }
    }

    private fun selectPunctuation(punctuated: Boolean) {
        val available = if (punctuated) currentState.punctuatedAvailable else currentState.plainAvailable
        // Недоступный вариант тихо подменился бы другим, поэтому выбрать его нельзя.
        if (available) update { it.copy(punctuation = punctuated) }
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    private fun checkFiles() {
        if (currentState.checking) return
        updateState { copy(checking = true, checkProgress = null) }
        viewModelScope.launch {
            try {
                prepareModels().collect(::onPreparation)
            } catch (error: IOException) {
                val reason = error.message?.let { ": $it" }.orEmpty()
                sendEffect(SettingsEffect.ShowMessage("Не удалось скопировать модели$reason", success = false))
            } finally {
                refreshStorage()
            }
        }
    }

    private fun onPreparation(event: ModelPreparation) {
        when (event) {
            is ModelPreparation.Copying -> updateState {
                copy(checkProgress = if (event.totalBytes > 0) event.copiedBytes.toFloat() / event.totalBytes else null)
            }
            is ModelPreparation.Done -> sendEffect(SettingsEffect.ShowMessage("Все модели на месте", success = true))
            is ModelPreparation.Missing -> sendEffect(
                SettingsEffect.ShowMessage(
                    "Не найдены веса: ${event.packs.joinToString { it.title }}",
                    success = false,
                ),
            )
        }
    }

    private fun refreshStorage() {
        updateState {
            copy(
                storage = getStorageStatus(),
                punctuatedAvailable = recognizer.supportsPunctuationMode(true),
                plainAvailable = recognizer.supportsPunctuationMode(false),
                checking = false,
                checkProgress = null,
            )
        }
    }

    private companion object {
        fun initialState(
            repository: SettingsRepository,
            recognizer: SpeechRecognizer,
            getStorageStatus: GetModelStorageStatusUseCase,
        ): SettingsState {
            val settings = repository.settings.value
            return SettingsState(
                punctuation = settings.punctuation,
                themeMode = settings.themeMode,
                highlightEmotions = settings.highlightEmotions,
                punctuatedAvailable = recognizer.supportsPunctuationMode(true),
                plainAvailable = recognizer.supportsPunctuationMode(false),
                storage = getStorageStatus(),
            )
        }
    }
}
