package ru.nuclearhack.stenographer.feature.onboarding.presentation

import ru.nuclearhack.stenographer.core.domain.engine.EngineKind

/** Шаги онбординга по порядку: знакомство, приватность, подготовка моделей, микрофон. */
enum class OnboardingStep { Welcome, Privacy, Models, Microphone }

/** Состояние одной модели в списке шага «Подготовка моделей». */
enum class ModelRowStatus { Pending, Active, Done, Unavailable }

data class ModelRow(
    val kind: EngineKind,
    val title: String,
    val sizeBytes: Long,
    val status: ModelRowStatus,
)

/** Ход подготовки моделей. */
sealed interface ModelsState {
    /** Шаг с моделями ещё не открывали. */
    data object Idle : ModelsState

    data class Copying(val copiedBytes: Long, val totalBytes: Long) : ModelsState

    data object Done : ModelsState

    /** Весов нет даже в APK: копировать нечего, но продолжить можно. */
    data object Missing : ModelsState

    /** Копирование оборвалось (например, кончилось место). */
    data object Failed : ModelsState
}

data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.Welcome,
    val models: ModelsState = ModelsState.Idle,
    val rows: List<ModelRow> = emptyList(),
) {
    /** Сколько всего весов в приложении. */
    val totalBytes: Long get() = rows.sumOf { it.sizeBytes }

    /** Кнопка «Дальше» активна; на шаге моделей — только когда копирование закончено или бессмысленно. */
    val canProceed: Boolean
        get() = step != OnboardingStep.Models ||
            models == ModelsState.Done || models == ModelsState.Missing || models == ModelsState.Failed
}

sealed interface OnboardingAction {
    data object Next : OnboardingAction

    data object Back : OnboardingAction

    /** Завершить онбординг: «Пропустить», «Не сейчас» или ответ на запрос микрофона. */
    data object Complete : OnboardingAction

    data object RetryCopy : OnboardingAction
}

sealed interface OnboardingEffect {
    data object Finish : OnboardingEffect
}
