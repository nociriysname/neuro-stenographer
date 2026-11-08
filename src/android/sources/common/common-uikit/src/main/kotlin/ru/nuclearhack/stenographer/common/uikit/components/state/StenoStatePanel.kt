package ru.nuclearhack.stenographer.common.uikit.components.state

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoButton
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoButtonSize
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoButtonVariant
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIllustrations
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/**
 * Панель пустого состояния или ошибки по центру: иллюстрация, заголовок, пояснение и действия во всю ширину.
 * Иллюстрация декоративна, смысл несёт заголовок.
 */
@Composable
fun StenoStatePanel(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    illustration: StenoIllustrations? = null,
    primaryAction: StenoStateAction? = null,
    secondaryAction: StenoStateAction? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (illustration != null) {
            Image(
                painter = illustration.painter,
                contentDescription = null,
                modifier = Modifier.widthIn(max = 240.dp).fillMaxWidth().aspectRatio(240f / 180f),
                contentScale = ContentScale.Fit,
            )
        }
        StenoText(
            title,
            modifier = Modifier.padding(top = 12.dp).semantics { heading() },
            style = StenoTheme.typography.title,
            textAlign = TextAlign.Center,
        )
        if (message != null) {
            StenoText(
                message,
                modifier = Modifier.padding(top = 8.dp),
                color = StenoTheme.colors.text2,
                textAlign = TextAlign.Center,
            )
        }
        if (primaryAction != null || secondaryAction != null) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (primaryAction != null) {
                    StenoButton(
                        text = primaryAction.text,
                        onClick = primaryAction.onClick,
                        size = StenoButtonSize.Large,
                        icon = primaryAction.icon,
                        fillWidth = true,
                    )
                }
                if (secondaryAction != null) {
                    StenoButton(
                        text = secondaryAction.text,
                        onClick = secondaryAction.onClick,
                        variant = StenoButtonVariant.Secondary,
                        icon = secondaryAction.icon,
                        fillWidth = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatePanelPreview(dark: Boolean) {
    StenoThemedSurface(darkTheme = dark) {
        StenoStatePanel(
            title = "Формат не поддерживается",
            message = "Выберите файл WAV, MP3, M4A или OGG.",
            illustration = StenoIllustrations.ErrorFormat,
            primaryAction = StenoStateAction("Выбрать другой файл", {}, StenoIcons.Import),
            secondaryAction = StenoStateAction("Отмена", {}),
        )
    }
}

@Preview
@Composable
private fun StenoStatePanelLightPreview() = StatePanelPreview(false)

@Preview
@Composable
private fun StenoStatePanelDarkPreview() = StatePanelPreview(true)
