package ru.nuclearhack.stenographer.common.uikit.components.control

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
 * Карточка выбора одного варианта из нескольких (язык, качество, формат экспорта). Справа квадратная «галочка»
 * вместо круглой радиокнопки; выбранная карточка получает рамку и подложку primary.
 */
@Composable
fun StenoOptionCard(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = StenoTheme.colors
    val shape = RoundedCornerShape(16.dp)
    // Выбранная рамка 2 dp (1 dp рамки + 1 dp внутренней обводки из макета); отступ компенсирует разницу.
    val borderWidth = if (selected) 2.dp else 1.dp
    Row(
        modifier = modifier
            .fillMaxWidth()
            .stenoFocusRing(shape)
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(if (selected) colors.primarySoft else colors.bg)
            .border(borderWidth, if (selected) colors.primary else colors.lineStrong, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp - borderWidth, vertical = 10.dp - borderWidth),
        horizontalArrangement = Arrangement.spacedBy(StenoTheme.spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            StenoText(title, style = StenoTheme.typography.labelLarge)
            if (subtitle != null) {
                StenoText(subtitle, style = StenoTheme.typography.bodySmall, color = colors.text2)
            }
        }
        CheckSquare(selected)
    }
}

@Composable
private fun CheckSquare(selected: Boolean) {
    val colors = StenoTheme.colors
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(22.dp)
            .background(if (selected) colors.primary else Color.Transparent, shape)
            .border(2.dp, if (selected) colors.primary else colors.lineStrong, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) StenoIcon(StenoIcons.Check, contentDescription = null, tint = colors.onPrimary, size = 16.dp)
    }
}

@Composable
private fun OptionCardPreviewContent() {
    var chosen by remember { mutableIntStateOf(1) }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StenoOptionCard("Быстро", "Короче ждать, чуть ниже точность", selected = chosen == 0, onClick = { chosen = 0 })
        StenoOptionCard("Точно", "Дольше, но аккуратнее", selected = chosen == 1, onClick = { chosen = 1 })
        StenoOptionCard("Только заголовок", null, selected = chosen == 2, onClick = { chosen = 2 })
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun StenoOptionCardLightPreview() {
    StenoThemedSurface(darkTheme = false) { OptionCardPreviewContent() }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun StenoOptionCardDarkPreview() {
    StenoThemedSurface(darkTheme = true) { OptionCardPreviewContent() }
}
