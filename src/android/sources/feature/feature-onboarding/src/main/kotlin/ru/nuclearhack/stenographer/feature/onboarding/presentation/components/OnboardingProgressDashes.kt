package ru.nuclearhack.stenographer.feature.onboarding.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import ru.nuclearhack.stenographer.common.uikit.components.progress.StenoLinearProgress
import ru.nuclearhack.stenographer.common.uikit.components.progress.StenoProgressHeight
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface
import ru.nuclearhack.stenographer.feature.onboarding.presentation.OnboardingStep

/** Сегментированный индикатор: закрашены все чёрточки до текущего шага включительно. */
@Composable
internal fun OnboardingProgressDashes(step: OnboardingStep, modifier: Modifier = Modifier) {
    val total = OnboardingStep.entries.size
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = StenoTheme.spacing.screen)
            .semantics { stateDescription = "Шаг ${step.ordinal + 1} из $total" },
        horizontalArrangement = Arrangement.spacedBy(StenoTheme.spacing.s2),
    ) {
        OnboardingStep.entries.forEach { item ->
            StenoLinearProgress(
                progress = if (item.ordinal <= step.ordinal) 1f else 0f,
                modifier = Modifier.weight(1f),
                height = StenoProgressHeight.Small,
            )
        }
    }
}

@Preview
@Composable
private fun DashesLightPreview() = StenoThemedSurface(darkTheme = false) {
    OnboardingProgressDashes(OnboardingStep.Models)
}

@Preview
@Composable
private fun DashesDarkPreview() = StenoThemedSurface(darkTheme = true) {
    OnboardingProgressDashes(OnboardingStep.Models)
}
