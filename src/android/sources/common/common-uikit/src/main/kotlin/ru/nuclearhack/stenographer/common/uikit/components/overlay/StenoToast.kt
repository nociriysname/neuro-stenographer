package ru.nuclearhack.stenographer.common.uikit.components.overlay

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoIcon
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/** Тост: сообщение с необязательными иконкой и действием («Отменить», «Повторить»); озвучивается диктором. */
@Composable
fun StenoToast(
    message: String,
    modifier: Modifier = Modifier,
    icon: StenoIcons? = null,
    iconTint: Color = StenoTheme.colors.onToast,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val colors = StenoTheme.colors
    val shape = StenoTheme.shapes.md
    val text = StenoTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .dropShadow(shape, StenoTheme.elevation.popup)
            .clip(shape)
            .background(colors.toast)
            .heightIn(min = 52.dp)
            .padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = if (actionLabel == null) 16.dp else 4.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) StenoIcon(icon, contentDescription = null, tint = iconTint)
        StenoText(message, modifier = Modifier.weight(1f), style = text, color = colors.onToast)
        if (actionLabel != null) {
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current,
                        role = Role.Button,
                        onClick = onAction,
                    )
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                StenoText(
                    actionLabel,
                    style = text.copy(fontWeight = FontWeight.Bold),
                    color = colors.toastAction,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ToastPreview(dark: Boolean) {
    StenoThemedSurface(darkTheme = dark) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StenoToast(
                message = "Протокол скопирован",
                icon = StenoIcons.Check,
                iconTint = StenoToastTone.Success.iconColor(),
                actionLabel = "Отменить",
            )
            StenoToast(
                message = "Не удалось сохранить файл",
                icon = StenoIcons.Warning,
                iconTint = StenoToastTone.Error.iconColor(),
                actionLabel = "Повторить",
            )
            StenoToast("Модель загружена", icon = StenoIcons.Info, iconTint = StenoToastTone.Info.iconColor())
        }
    }
}

@Preview
@Composable
private fun StenoToastLightPreview() = ToastPreview(false)

@Preview
@Composable
private fun StenoToastDarkPreview() = ToastPreview(true)
