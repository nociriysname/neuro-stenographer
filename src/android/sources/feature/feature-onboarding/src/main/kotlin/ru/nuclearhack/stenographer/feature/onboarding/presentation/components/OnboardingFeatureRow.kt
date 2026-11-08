package ru.nuclearhack.stenographer.feature.onboarding.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ru.nuclearhack.stenographer.common.uikit.components.surface.StenoIconTile
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/** Строка преимущества: плитка с иконкой, заголовок и пояснение. */
@Composable
internal fun OnboardingFeatureRow(
    icon: StenoIcons,
    title: String,
    caption: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = StenoTheme.spacing.s12 + StenoTheme.spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(StenoTheme.spacing.s3),
    ) {
        StenoIconTile(icon, size = StenoTheme.spacing.s10)
        Column(Modifier.weight(1f)) {
            StenoText(title, style = StenoTheme.typography.label)
            StenoText(caption, style = StenoTheme.typography.caption, color = StenoTheme.colors.text2)
        }
    }
}

@Preview
@Composable
private fun FeatureRowLightPreview() = StenoThemedSurface(darkTheme = false) {
    OnboardingFeatureRow(StenoIcons.Speaker, "Кто говорит", "голоса разделены по людям")
}

@Preview
@Composable
private fun FeatureRowDarkPreview() = StenoThemedSurface(darkTheme = true) {
    OnboardingFeatureRow(StenoIcons.Speaker, "Кто говорит", "голоса разделены по людям")
}
