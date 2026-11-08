package ru.nuclearhack.stenographer.feature.onboarding.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoIcon
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/**
 * Большая квадратная кнопка 64 dp из макета онбординга. В uikit такой нет, поэтому собрана из токенов:
 * это единственное главное действие шага. Стрелка вправо — это иконка «назад», повёрнутая на пол-оборота.
 */
@Composable
internal fun OnboardingRoundButton(
    icon: StenoIcons,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pointRight: Boolean = false,
) {
    val colors = StenoTheme.colors
    Box(
        modifier = modifier
            .size(StenoTheme.spacing.s16)
            .clip(StenoTheme.shapes.xl)
            .background(if (enabled) colors.primary else colors.surface2)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        StenoIcon(
            icon = icon,
            contentDescription = description,
            modifier = if (pointRight) Modifier.rotate(180f) else Modifier,
            tint = if (enabled) colors.onPrimary else colors.text3,
        )
    }
}

private fun previewNoop() = Unit

@Preview
@Composable
private fun RoundButtonLightPreview() = StenoThemedSurface(darkTheme = false) {
    OnboardingRoundButton(StenoIcons.Back, "Дальше", ::previewNoop, pointRight = true)
}

@Preview
@Composable
private fun RoundButtonDarkDisabledPreview() = StenoThemedSurface(darkTheme = true) {
    OnboardingRoundButton(StenoIcons.Back, "Дальше", ::previewNoop, enabled = false, pointRight = true)
}
