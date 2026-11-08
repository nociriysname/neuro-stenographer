package ru.nuclearhack.stenographer.feature.onboarding.presentation

import android.Manifest
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.metroViewModel
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoButton
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoButtonSize
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoButtonVariant
import ru.nuclearhack.stenographer.common.uikit.components.scaffold.StenoScaffold
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface
import ru.nuclearhack.stenographer.common.uikit.udf.CollectEffects
import ru.nuclearhack.stenographer.core.domain.engine.EngineKind
import ru.nuclearhack.stenographer.feature.onboarding.presentation.components.OnboardingProgressDashes
import ru.nuclearhack.stenographer.feature.onboarding.presentation.components.OnboardingRoundButton
import ru.nuclearhack.stenographer.feature.onboarding.presentation.components.OnboardingStepPage

/** Онбординг в 4 шага (S01). [onFinished] — пользователь прошёл или пропустил онбординг. */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Что бы ни ответил пользователь, онбординг на этом закончен: отказ не блокирует приложение.
    val requestMicrophone = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onAction(OnboardingAction.Complete)
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            OnboardingEffect.Finish -> onFinished()
        }
    }
    BackHandler(enabled = state.step != OnboardingStep.Welcome) { viewModel.onAction(OnboardingAction.Back) }

    OnboardingContent(
        state = state,
        onNext = { viewModel.onAction(OnboardingAction.Next) },
        onSkip = { viewModel.onAction(OnboardingAction.Complete) },
        onRetryCopy = { viewModel.onAction(OnboardingAction.RetryCopy) },
        onRequestMicrophone = { requestMicrophone.launch(Manifest.permission.RECORD_AUDIO) },
    )
}

@Composable
private fun OnboardingContent(
    state: OnboardingState,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onRetryCopy: () -> Unit,
    onRequestMicrophone: () -> Unit,
) {
    val motion = StenoTheme.motion
    StenoScaffold(
        topBar = {
            OnboardingProgressDashes(
                step = state.step,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = StenoTheme.spacing.s6, bottom = StenoTheme.spacing.s3),
            )
        },
        bottomBar = {
            OnboardingBottomBar(
                step = state.step,
                canProceed = state.canProceed,
                onNext = onNext,
                onSkip = onSkip,
                onRequestMicrophone = onRequestMicrophone,
            )
        },
    ) {
        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                (slideInHorizontally(motion.base()) { if (forward) it else -it } + fadeIn(motion.base())) togetherWith
                    (slideOutHorizontally(motion.base()) { if (forward) -it else it } + fadeOut(motion.base()))
            },
            label = "onboardingStep",
        ) { step ->
            OnboardingStepPage(step, state, onRetryCopy)
        }
    }
}

@Composable
private fun OnboardingBottomBar(
    step: OnboardingStep,
    canProceed: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onRequestMicrophone: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(
                horizontal = StenoTheme.spacing.screen,
                vertical = StenoTheme.spacing.s3,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(StenoTheme.spacing.s2),
    ) {
        StenoButton(
            text = if (step == OnboardingStep.Microphone) "Не сейчас" else "Пропустить",
            onClick = onSkip,
            modifier = Modifier.weight(1f),
            variant = StenoButtonVariant.Text,
            size = StenoButtonSize.Large,
            fillWidth = true,
        )
        if (step == OnboardingStep.Microphone) {
            OnboardingRoundButton(StenoIcons.Mic, "Разрешить доступ к микрофону", onRequestMicrophone)
        } else {
            OnboardingRoundButton(
                icon = StenoIcons.Back,
                description = if (canProceed) "Дальше" else "Дальше: ждём копирование моделей",
                onClick = onNext,
                enabled = canProceed,
                pointRight = true,
            )
        }
    }
}

private fun previewNoop() = Unit

private val previewRows = listOf(
    ModelRow(EngineKind.Diarization, "Определение говорящих", 107_000_000, ModelRowStatus.Done),
    ModelRow(EngineKind.SpeechRecognition, "Распознавание речи", 232_000_000, ModelRowStatus.Active),
)

private val previewStates = listOf(
    OnboardingState(OnboardingStep.Welcome),
    OnboardingState(OnboardingStep.Privacy),
    OnboardingState(OnboardingStep.Models, ModelsState.Copying(150_000_000, 339_000_000), previewRows),
    OnboardingState(
        OnboardingStep.Models,
        ModelsState.Done,
        previewRows.map { it.copy(status = ModelRowStatus.Done) },
    ),
    OnboardingState(
        OnboardingStep.Models,
        ModelsState.Missing,
        previewRows.map { it.copy(status = ModelRowStatus.Unavailable) },
    ),
    OnboardingState(OnboardingStep.Microphone),
)

@Composable
private fun OnboardingPreview(index: Int, dark: Boolean) {
    StenoThemedSurface(darkTheme = dark) {
        OnboardingContent(
            state = previewStates[index],
            onNext = ::previewNoop,
            onSkip = ::previewNoop,
            onRetryCopy = ::previewNoop,
            onRequestMicrophone = ::previewNoop,
        )
    }
}

@Preview(name = "1 светлая", showBackground = true, heightDp = 780)
@Composable
private fun Step1LightPreview() = OnboardingPreview(0, dark = false)

@Preview(name = "1 тёмная", showBackground = true, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Step1DarkPreview() = OnboardingPreview(0, dark = true)

@Preview(name = "2 светлая", showBackground = true, heightDp = 780)
@Composable
private fun Step2LightPreview() = OnboardingPreview(1, dark = false)

@Preview(name = "2 тёмная", showBackground = true, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Step2DarkPreview() = OnboardingPreview(1, dark = true)

@Preview(name = "3 копирование светлая", showBackground = true, heightDp = 780)
@Composable
private fun Step3CopyingLightPreview() = OnboardingPreview(2, dark = false)

@Preview(name = "3 копирование тёмная", showBackground = true, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Step3CopyingDarkPreview() = OnboardingPreview(2, dark = true)

@Preview(name = "3 готово светлая", showBackground = true, heightDp = 780)
@Composable
private fun Step3DoneLightPreview() = OnboardingPreview(3, dark = false)

@Preview(name = "3 готово тёмная", showBackground = true, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Step3DoneDarkPreview() = OnboardingPreview(3, dark = true)

@Preview(name = "3 нет моделей светлая", showBackground = true, heightDp = 780)
@Composable
private fun Step3MissingLightPreview() = OnboardingPreview(4, dark = false)

@Preview(name = "3 нет моделей тёмная", showBackground = true, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Step3MissingDarkPreview() = OnboardingPreview(4, dark = true)

@Preview(name = "4 светлая", showBackground = true, heightDp = 780)
@Composable
private fun Step4LightPreview() = OnboardingPreview(5, dark = false)

@Preview(name = "4 тёмная", showBackground = true, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Step4DarkPreview() = OnboardingPreview(5, dark = true)
