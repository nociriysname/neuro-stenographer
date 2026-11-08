package ru.nuclearhack.stenographer.common.uikit.components.bar

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoIcon
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/**
 * Нижняя навигация с кнопкой записи в «выемке» по центру. Первая половина [items] слева от кнопки, вторая справа;
 * в макете 2 + 2. При [recording] кнопка красная со «стопом». Отступ под системную панель добавляется сама.
 */
@Composable
fun StenoBottomBar(
    items: List<StenoBottomBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onRecordClick: () -> Unit,
    modifier: Modifier = Modifier,
    recording: Boolean = false,
) {
    val colors = StenoTheme.colors
    val half = items.size / 2
    // Верхние 26 dp — прозрачная зона под выступающую кнопку, чтобы она принимала касания внутри границ панели.
    Box(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(top = 26.dp)
                .background(colors.bg)
                .drawBehind {
                    drawLine(colors.line, Offset.Zero, Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
                }
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 6.dp, end = 6.dp, bottom = 4.dp)
                .heightIn(min = 76.dp)
                .height(IntrinsicSize.Min),
        ) {
            items.take(half).forEachIndexed { index, item ->
                BarItem(item, selected = index == selectedIndex, onClick = { onSelect(index) })
            }
            Box(Modifier.width(80.dp).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                StenoText(
                    "Запись",
                    modifier = Modifier.padding(bottom = 10.dp),
                    style = StenoTheme.typography.overline.copy(letterSpacing = TextUnit.Unspecified),
                    color = colors.text2,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
            items.drop(half).forEachIndexed { index, item ->
                BarItem(item, selected = half + index == selectedIndex, onClick = { onSelect(half + index) })
            }
        }
        RecordNotch(recording, onRecordClick, Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun RowScope.BarItem(item: StenoBottomBarItem, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) StenoTheme.colors.primaryInk else StenoTheme.colors.text2
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .widthIn(min = 48.dp)
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                role = Role.Tab,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        StenoIcon(item.icon, contentDescription = null, tint = tint)
        StenoText(
            item.label,
            style = StenoTheme.typography.overline.copy(letterSpacing = TextUnit.Unspecified),
            color = tint,
            maxLines = 1,
        )
    }
}

@Composable
private fun RecordNotch(recording: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = StenoTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val base = if (recording) colors.rec else colors.primary
    val fill = if (pressed && !recording) colors.primaryPress else base
    val outer = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .size(68.dp)
            .dropShadow(outer, Shadow(radius = 16.dp, color = base.copy(alpha = 0.32f), offset = DpOffset(0.dp, 6.dp)))
            .clip(outer)
            .background(colors.bg)
            .padding(5.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(fill)
            .semantics { contentDescription = if (recording) "Остановить запись" else "Начать запись" }
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        StenoIcon(
            icon = if (recording) StenoIcons.Stop else StenoIcons.Mic,
            contentDescription = null,
            tint = if (recording) Color.White else colors.onPrimary,
            size = 32.dp,
        )
    }
}

@Composable
private fun BottomBarPreview(dark: Boolean) {
    val items = listOf(
        StenoBottomBarItem("Главная", StenoIcons.Home),
        StenoBottomBarItem("История", StenoIcons.History),
        StenoBottomBarItem("Импорт", StenoIcons.Import),
        StenoBottomBarItem("Настройки", StenoIcons.Settings),
    )
    StenoThemedSurface(darkTheme = dark) {
        Column {
            StenoBottomBar(items, selectedIndex = 0, onSelect = {}, onRecordClick = {})
            StenoBottomBar(items, selectedIndex = 1, onSelect = {}, onRecordClick = {}, recording = true)
        }
    }
}

@Preview
@Composable
private fun StenoBottomBarLightPreview() = BottomBarPreview(false)

@Preview
@Composable
private fun StenoBottomBarDarkPreview() = BottomBarPreview(true)
