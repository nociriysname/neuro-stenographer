package ru.nuclearhack.stenographer.common.uikit.components.bar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoIconButton
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoIconButtonVariant
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/**
 * Верхняя панель. Главная ([onBack] = null): «глазок» [eyebrow] капсом и заголовок 24.
 * Вложенная: кнопка «назад» и заголовок 20 в одну строку. Отступ под статус-бар добавляется сама.
 */
@Composable
fun StenoTopBar(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val typography = StenoTheme.typography
    val nested = onBack != null
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .heightIn(min = 72.dp)
            .padding(start = if (nested) 8.dp else 20.dp, top = 12.dp, end = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            StenoIconButton(
                icon = StenoIcons.Back,
                contentDescription = "Назад",
                onClick = onBack,
                variant = StenoIconButtonVariant.Ghost,
            )
        }
        Column(Modifier.weight(1f)) {
            if (!nested && eyebrow != null) {
                StenoText(
                    eyebrow.uppercase(),
                    style = typography.overline,
                    color = StenoTheme.colors.text2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StenoText(
                title,
                modifier = Modifier.semantics { heading() },
                style = if (nested) typography.title.copy(fontSize = 20.sp, lineHeight = 26.sp) else typography.title,
                maxLines = if (nested) 1 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis,
            )
        }
        actions()
    }
}

@Composable
private fun TopBarPreview(dark: Boolean) {
    StenoThemedSurface(darkTheme = dark) {
        Column {
            StenoTopBar(
                title = "Главная",
                eyebrow = "12 января · 3 расшифровки",
                actions = {
                    StenoIconButton(
                        icon = StenoIcons.Settings,
                        contentDescription = "Настройки",
                        onClick = ::previewNoop,
                        variant = StenoIconButtonVariant.Ghost,
                    )
                },
            )
            StenoTopBar(
                title = "Планёрка отдела разработки",
                onBack = {},
                actions = {
                    StenoIconButton(
                        icon = StenoIcons.Search,
                        contentDescription = "Поиск",
                        onClick = ::previewNoop,
                        variant = StenoIconButtonVariant.Ghost,
                    )
                    StenoIconButton(
                        icon = StenoIcons.More,
                        contentDescription = "Ещё",
                        onClick = ::previewNoop,
                        variant = StenoIconButtonVariant.Ghost,
                    )
                },
            )
        }
    }
}

@Preview
@Composable
private fun StenoTopBarLightPreview() = TopBarPreview(false)

@Preview
@Composable
private fun StenoTopBarDarkPreview() = TopBarPreview(true)

private fun previewNoop() = Unit
