package ru.nuclearhack.stenographer.common.uikit.components.field

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoIcon
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/**
 * Текстовое поле с подписью над ним (имя говорящего, название записи). Подпись видна всегда,
 * а не прячется в placeholder, чтобы её не терять при вводе.
 */
@Composable
fun StenoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    leading: StenoIcons? = null,
) {
    val colors = StenoTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) StenoText(label, style = StenoTheme.typography.label, color = colors.text2)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            textStyle = StenoTheme.typography.body.copy(color = colors.text),
            cursorBrush = SolidColor(colors.primary),
            interactionSource = interaction,
            decorationBox = { innerTextField ->
                StenoFieldContainer(
                    focused = focused,
                    modifier = if (singleLine) Modifier else Modifier.padding(vertical = 12.dp),
                    verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
                ) {
                    if (leading != null) StenoIcon(leading, contentDescription = null, tint = colors.text2)
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder != null) {
                            StenoText(placeholder, style = StenoTheme.typography.body, color = colors.text3)
                        }
                        innerTextField()
                    }
                }
            },
        )
    }
}

@Composable
private fun TextFieldPreviewContent() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StenoTextField("Игорь Петров", {}, label = "Имя")
        StenoTextField("", {}, label = "Название", placeholder = "Планёрка", leading = StenoIcons.Edit)
        StenoTextField("", {}, placeholder = "Без подписи")
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun StenoTextFieldLightPreview() {
    StenoThemedSurface(darkTheme = false) { TextFieldPreviewContent() }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun StenoTextFieldDarkPreview() {
    StenoThemedSurface(darkTheme = true) { TextFieldPreviewContent() }
}
