package ru.nuclearhack.stenographer.common.uikit.components.surface

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/** Карточка: скругление 18, тонкая рамка; при [onClick] становится кнопкой с ripple. */
@Composable
fun StenoCard(
    modifier: Modifier = Modifier,
    style: StenoCardStyle = StenoCardStyle.Raised,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = StenoTheme.colors
    val shape = StenoTheme.shapes.lg
    val raised = style == StenoCardStyle.Raised
    Column(
        modifier = modifier
            .then(if (raised) Modifier.dropShadow(shape, StenoTheme.elevation.card) else Modifier)
            .clip(shape)
            .background(if (raised) colors.bg else colors.surface1)
            .border(BorderStroke(1.dp, colors.line), shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current,
                        role = Role.Button,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(contentPadding),
        content = content,
    )
}

@Composable
private fun CardPreview(dark: Boolean) {
    StenoThemedSurface(darkTheme = dark) {
        Column(Modifier.padding(16.dp)) {
            StenoCard(onClick = ::previewNoop) { StenoText("Карточка с нажатием") }
            StenoCard(Modifier.padding(top = 12.dp), style = StenoCardStyle.Flat) { StenoText("Плоская карточка") }
        }
    }
}

@Preview
@Composable
private fun StenoCardLightPreview() = CardPreview(false)

@Preview
@Composable
private fun StenoCardDarkPreview() = CardPreview(true)

private fun previewNoop() = Unit
