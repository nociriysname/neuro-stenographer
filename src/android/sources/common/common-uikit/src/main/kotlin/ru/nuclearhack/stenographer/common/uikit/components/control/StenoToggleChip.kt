package ru.nuclearhack.stenographer.common.uikit.components.control

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.components.modifier.stenoFocusRing
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoIcon
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/**
 * Чип-переключатель для фильтров и опций («Только мои», «С эмоциями»). Видимая высота 40 dp, зона касания 48 dp.
 */
@Composable
fun StenoToggleChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: StenoIcons? = null,
) {
    val colors = StenoTheme.colors
    val shape = StenoTheme.shapes.sm
    val interaction = remember { MutableInteractionSource() }
    val content = if (selected) colors.primaryInk else colors.text

    Box(
        modifier = modifier
            .stenoFocusRing(shape)
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .toggleable(
                value = selected,
                interactionSource = interaction,
                indication = null,
                role = Role.Checkbox,
                onValueChange = { onClick() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 40.dp)
                .sizeIn(minWidth = 48.dp)
                .clip(shape)
                .background(if (selected) colors.primarySoft else colors.bg)
                .border(1.dp, if (selected) colors.primary else colors.lineStrong, shape)
                .indication(interaction, LocalIndication.current)
                .padding(horizontal = StenoTheme.spacing.s3),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) StenoIcon(icon, contentDescription = null, tint = content, size = 20.dp)
            StenoText(text, style = StenoTheme.typography.label, color = content, maxLines = 1)
        }
    }
}

@Composable
private fun ToggleChipPreviewContent() {
    var first by remember { mutableStateOf(true) }
    var second by remember { mutableStateOf(false) }
    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StenoToggleChip("Все", selected = first, onClick = { first = !first })
        StenoToggleChip("Мои", selected = second, onClick = { second = !second })
        StenoToggleChip("Эмоции", selected = false, onClick = { second = !second }, icon = StenoIcons.Smile)
        StenoToggleChip("Эмоции", selected = true, onClick = { first = !first }, icon = StenoIcons.Smile)
    }
}

@Preview(showBackground = true, widthDp = 420)
@Composable
private fun StenoToggleChipLightPreview() {
    StenoThemedSurface(darkTheme = false) { ToggleChipPreviewContent() }
}

@Preview(showBackground = true, widthDp = 420)
@Composable
private fun StenoToggleChipDarkPreview() {
    StenoThemedSurface(darkTheme = true) { ToggleChipPreviewContent() }
}
