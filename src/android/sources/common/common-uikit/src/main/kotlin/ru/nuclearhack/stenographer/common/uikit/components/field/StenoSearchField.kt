package ru.nuclearhack.stenographer.common.uikit.components.field

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoIconButton
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoIconButtonSize
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoIconButtonVariant
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoIcon
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.tabular

/**
 * Поле поиска по тексту расшифровки. [matchCounter] — готовая строка вида «2 из 5», показывается справа;
 * кнопка очистки появляется, когда запрос не пуст.
 */
@Composable
fun StenoSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Поиск по тексту",
    matchCounter: String? = null,
    onClear: () -> Unit = { onQueryChange("") },
) {
    val colors = StenoTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = StenoTheme.typography.body.copy(color = colors.text),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        interactionSource = interaction,
        decorationBox = { innerTextField ->
            StenoFieldContainer(focused = focused) {
                StenoIcon(StenoIcons.Search, contentDescription = null, tint = colors.text2)
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        StenoText(placeholder, style = StenoTheme.typography.body, color = colors.text3, maxLines = 1)
                    }
                    innerTextField()
                }
                if (matchCounter != null && query.isNotEmpty()) {
                    StenoText(matchCounter, style = StenoTheme.typography.caption.tabular(), color = colors.text2)
                }
                if (query.isNotEmpty()) {
                    StenoIconButton(
                        icon = StenoIcons.Close,
                        contentDescription = "Очистить поиск",
                        onClick = onClear,
                        variant = StenoIconButtonVariant.Ghost,
                        size = StenoIconButtonSize.Small,
                    )
                }
            }
        },
    )
}

@Composable
private fun SearchFieldPreviewContent() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StenoSearchField(query = "", onQueryChange = {})
        StenoSearchField(query = "бюджет", onQueryChange = {}, matchCounter = "2 из 5")
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun StenoSearchFieldLightPreview() {
    StenoThemedSurface(darkTheme = false) { SearchFieldPreviewContent() }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun StenoSearchFieldDarkPreview() {
    StenoThemedSurface(darkTheme = true) { SearchFieldPreviewContent() }
}
