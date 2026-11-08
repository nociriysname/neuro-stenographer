package ru.nuclearhack.stenographer.common.uikit.components.surface

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoIcon
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/** Строка списка: ведущий и замыкающий слоты, заголовок и подпись. Разделитель — отдельный [StenoDivider]. */
@Composable
fun StenoListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
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
            .heightIn(min = 64.dp)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            StenoText(title, style = StenoTheme.typography.labelLarge)
            if (subtitle != null) {
                StenoText(
                    subtitle,
                    style = StenoTheme.typography.caption,
                    color = StenoTheme.colors.text2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
private fun ListRowPreview(dark: Boolean) {
    StenoThemedSurface(darkTheme = dark) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            StenoListRow(
                title = "Планёрка 12 ноября",
                subtitle = "24 мин · 3 спикера",
                leading = { StenoIconTile(StenoIcons.AudioFile) },
                trailing = { StenoIcon(StenoIcons.Chevron, null, tint = StenoTheme.colors.text3) },
                onClick = ::previewNoop,
            )
            StenoDivider()
            StenoListRow(title = "Без подписи")
        }
    }
}

@Preview
@Composable
private fun StenoListRowLightPreview() = ListRowPreview(false)

@Preview
@Composable
private fun StenoListRowDarkPreview() = ListRowPreview(true)

private fun previewNoop() = Unit
