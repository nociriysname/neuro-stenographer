package ru.nuclearhack.stenographer.common.uikit.components.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoButton
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoButtonVariant
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/**
 * Диалог подтверждения по центру экрана: заголовок, пояснение и две кнопки в ряд.
 * При [destructive] подтверждение красное.
 */
@Composable
fun StenoDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
) {
    StenoOverlayWindow(visible, onDismissRequest, slowBody = false) { scrim, body ->
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { onDismissRequest() } },
        ) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = scrim() }.background(StenoTheme.colors.scrim))
            Box(
                Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
                contentAlignment = Alignment.Center,
            ) {
                StenoDialogContent(
                    title = title,
                    message = message,
                    confirmText = confirmText,
                    onConfirm = onConfirm,
                    dismissText = dismissText,
                    onDismiss = onDismissRequest,
                    destructive = destructive,
                    modifier = modifier
                        .padding(horizontal = 24.dp)
                        .widthIn(max = 560.dp)
                        .graphicsLayer {
                            alpha = body()
                            scaleX = 0.94f + 0.06f * body()
                            scaleY = 0.94f + 0.06f * body()
                        }
                        .pointerInput(Unit) { detectTapGestures { } },
                )
            }
        }
    }
}

/** Статичное тело диалога без окна и анимаций. */
@Composable
internal fun StenoDialogContent(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String,
    onDismiss: () -> Unit,
    destructive: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = StenoTheme.shapes.xl
    Column(
        modifier = modifier
            .fillMaxWidth()
            .dropShadow(shape, StenoTheme.elevation.popup)
            .clip(shape)
            .background(StenoTheme.colors.bg)
            .border(1.dp, StenoTheme.colors.line, shape)
            .padding(24.dp)
            .semantics { paneTitle = title },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StenoText(title, style = StenoTheme.typography.subtitle)
        StenoText(message, color = StenoTheme.colors.text2)
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StenoButton(
                text = dismissText,
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                variant = StenoButtonVariant.Secondary,
                fillWidth = true,
            )
            StenoButton(
                text = confirmText,
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                variant = if (destructive) StenoButtonVariant.Danger else StenoButtonVariant.Primary,
                fillWidth = true,
            )
        }
    }
}

@Composable
private fun DialogPreview(dark: Boolean) {
    StenoThemedSurface(darkTheme = dark) {
        StenoDialogContent(
            title = "Удалить расшифровку?",
            message = "Текст и аудио исчезнут с устройства, вернуть их не получится.",
            confirmText = "Удалить",
            onConfirm = {},
            dismissText = "Отмена",
            onDismiss = {},
            destructive = true,
            modifier = Modifier.padding(24.dp),
        )
    }
}

@Preview
@Composable
private fun StenoDialogLightPreview() = DialogPreview(false)

@Preview
@Composable
private fun StenoDialogDarkPreview() = DialogPreview(true)
