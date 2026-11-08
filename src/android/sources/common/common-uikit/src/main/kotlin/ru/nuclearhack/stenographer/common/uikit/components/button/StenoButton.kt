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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nuclearhack.stenographer.common.uikit.components.modifier.stenoFocusRing
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoIcon
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/** Вид кнопки: заливка, контур или только текст. */
enum class StenoButtonVariant { Primary, Secondary, Text, Danger, DangerText }

/** Размер кнопки; зона касания у всех не меньше 48 dp. */
enum class StenoButtonSize(
    internal val minHeight: Dp,
    internal val radius: Dp,
    internal val horizontalPadding: Dp,
) {
    Large(56.dp, 16.dp, 24.dp),
    Medium(48.dp, 14.dp, 20.dp),
    Small(40.dp, 12.dp, 14.dp),
}

/**
 * Кнопка дизайн-системы. Primary — главное действие экрана, Secondary — второстепенное, Text — третичное,
 * Danger/DangerText — необратимые действия. Высота минимальная: при крупном шрифте кнопка растёт.
 */
@Composable
fun StenoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: StenoButtonVariant = StenoButtonVariant.Primary,
    size: StenoButtonSize = StenoButtonSize.Medium,
    icon: StenoIcons? = null,
    enabled: Boolean = true,
    fillWidth: Boolean = false,
) {
    val colors = StenoTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shape = RoundedCornerShape(size.radius)
    val textOnly = variant == StenoButtonVariant.Text || variant == StenoButtonVariant.DangerText

    val background = when {
        !enabled -> if (textOnly) Color.Transparent else colors.surface2
        else -> when (variant) {
            StenoButtonVariant.Primary -> if (pressed) colors.primaryPress else colors.primary
            StenoButtonVariant.Secondary -> if (pressed) colors.surface2 else colors.bg
            StenoButtonVariant.Text -> if (pressed) colors.primarySoft else Color.Transparent
            StenoButtonVariant.Danger -> if (pressed) lerp(colors.error, Color.Black, 0.1f) else colors.error
            StenoButtonVariant.DangerText -> if (pressed) colors.errorSoft else Color.Transparent
        }
    }
    val content = when {
        !enabled -> colors.text3
        else -> when (variant) {
            StenoButtonVariant.Primary -> colors.onPrimary
            StenoButtonVariant.Secondary -> colors.text
            StenoButtonVariant.Text -> colors.primaryInk
            StenoButtonVariant.Danger -> colors.onError
            StenoButtonVariant.DangerText -> colors.errorInk
        }
    }
    val border = if (variant == StenoButtonVariant.Secondary && enabled) colors.lineStrong else Color.Transparent
    val hasShadow = variant == StenoButtonVariant.Primary && enabled
    val textStyle: TextStyle = when (size) {
        StenoButtonSize.Large -> StenoTheme.typography.labelLarge.copy(fontSize = 17.sp)
        StenoButtonSize.Medium -> StenoTheme.typography.labelLarge
        StenoButtonSize.Small -> StenoTheme.typography.label
    }
    val horizontal = if (textOnly) StenoTheme.spacing.s3 else size.horizontalPadding

    // Внешний слой — зона касания 48 dp, внутренний — видимая кнопка (у Small она 40 dp).
    Box(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .stenoFocusRing(shape)
            .heightIn(min = 48.dp)
            .widthIn(min = 48.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                .heightIn(min = size.minHeight)
                .then(
                    if (hasShadow) {
                        Modifier.dropShadow(
                            shape,
                            Shadow(
                                radius = 8.dp,
                                color = colors.primary.copy(alpha = 0.3f),
                                offset = DpOffset(0.dp, 2.dp),
                            ),
                        )
                    } else {
                        Modifier
                    },
                )
                .clip(shape)
                .background(background)
                .then(
                    if (hasShadow) {
                        Modifier.innerShadow(
                            shape,
                            Shadow(
                                radius = 0.dp,
                                color = Color.White.copy(alpha = 0.22f),
                                offset = DpOffset(0.dp, 1.dp),
                            ),
                        )
                    } else {
                        Modifier
                    },
                )
                .border(1.5.dp, border, shape)
                .indication(interaction, LocalIndication.current)
                .padding(horizontal = horizontal),
            horizontalArrangement = Arrangement.spacedBy(StenoTheme.spacing.s2, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) StenoIcon(icon, contentDescription = null, tint = content)
            StenoText(text, style = textStyle, color = content, maxLines = 1)
        }
    }
}

@Composable
private fun ButtonsPreviewContent() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StenoButtonVariant.entries.forEach { variant ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                StenoButton("Запись", {}, variant = variant, size = StenoButtonSize.Large)
                StenoButton("Запись", {}, variant = variant, icon = StenoIcons.Mic)
                StenoButton("Запись", {}, variant = variant, size = StenoButtonSize.Small)
            }
        }
        StenoButton("Отключена", {}, enabled = false)
        StenoButton("Отключена", {}, variant = StenoButtonVariant.Text, enabled = false)
        StenoButton("Во всю ширину", {}, fillWidth = true, icon = StenoIcons.Import, size = StenoButtonSize.Large)
    }
}

@Preview(showBackground = true, widthDp = 420)
@Composable
private fun StenoButtonLightPreview() {
    StenoThemedSurface(darkTheme = false) { ButtonsPreviewContent() }
}

@Preview(showBackground = true, widthDp = 420)
@Composable
private fun StenoButtonDarkPreview() {
    StenoThemedSurface(darkTheme = true) { ButtonsPreviewContent() }
}
