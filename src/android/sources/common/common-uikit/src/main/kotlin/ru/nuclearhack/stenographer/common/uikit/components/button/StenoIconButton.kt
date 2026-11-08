package ru.nuclearhack.stenographer.common.uikit.components.button

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.components.modifier.stenoFocusRing
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoIcon
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/** Вид иконочной кнопки: с контуром, прозрачная, заливка primary или «включена». */
enum class StenoIconButtonVariant { Outlined, Ghost, Primary, Selected }

/** Размер иконочной кнопки; зона касания у Small всё равно 48 dp. */
enum class StenoIconButtonSize(internal val side: Dp, internal val radius: Dp, internal val iconSize: Dp) {
    Medium(48.dp, 14.dp, 24.dp),
    Small(40.dp, 12.dp, 20.dp),
}

/**
 * Кнопка-иконка. [contentDescription] обязателен: это единственное, что услышит пользователь TalkBack.
 */
@Composable
fun StenoIconButton(
    icon: StenoIcons,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: StenoIconButtonVariant = StenoIconButtonVariant.Outlined,
    size: StenoIconButtonSize = StenoIconButtonSize.Medium,
    enabled: Boolean = true,
) {
    val colors = StenoTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(size.radius)

    val background = when {
        !enabled -> if (variant == StenoIconButtonVariant.Ghost) Color.Transparent else colors.surface1
        pressed -> when (variant) {
            StenoIconButtonVariant.Primary -> colors.primaryPress
            StenoIconButtonVariant.Selected -> colors.primarySoft
            else -> colors.surface2
        }
        else -> when (variant) {
            StenoIconButtonVariant.Outlined -> colors.bg
            StenoIconButtonVariant.Ghost -> Color.Transparent
            StenoIconButtonVariant.Primary -> colors.primary
            StenoIconButtonVariant.Selected -> colors.primarySoft
        }
    }
    val tint = when {
        !enabled -> colors.text3
        variant == StenoIconButtonVariant.Primary -> colors.onPrimary
        variant == StenoIconButtonVariant.Selected -> colors.primaryInk
        else -> colors.text
    }
    val border = when {
        variant == StenoIconButtonVariant.Selected && enabled -> colors.primary
        variant == StenoIconButtonVariant.Outlined -> colors.line
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .stenoFocusRing(shape)
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .semantics { if (variant == StenoIconButtonVariant.Selected) selected = true }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size.side)
                .clip(shape)
                .background(background)
                .border(1.dp, border, shape)
                .indication(interaction, LocalIndication.current),
            contentAlignment = Alignment.Center,
        ) {
            StenoIcon(icon, contentDescription, tint = tint, size = size.iconSize)
        }
    }
}

@Composable
private fun IconButtonsPreviewContent() {
    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StenoIconButtonVariant.entries.forEach { variant ->
            StenoIconButton(StenoIcons.Share, "Поделиться", {}, variant = variant)
            StenoIconButton(StenoIcons.Share, "Поделиться", {}, variant = variant, size = StenoIconButtonSize.Small)
        }
        StenoIconButton(StenoIcons.Share, "Поделиться", {}, enabled = false)
    }
}

@Preview(showBackground = true, widthDp = 520)
@Composable
private fun StenoIconButtonLightPreview() {
    StenoThemedSurface(darkTheme = false) { IconButtonsPreviewContent() }
}

@Preview(showBackground = true, widthDp = 520)
@Composable
private fun StenoIconButtonDarkPreview() {
    StenoThemedSurface(darkTheme = true) { IconButtonsPreviewContent() }
}
