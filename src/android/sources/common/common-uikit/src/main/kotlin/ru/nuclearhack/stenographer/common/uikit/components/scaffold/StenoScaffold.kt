package ru.nuclearhack.stenographer.common.uikit.components.scaffold

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.components.bar.StenoBottomBar
import ru.nuclearhack.stenographer.common.uikit.components.bar.StenoBottomBarItem
import ru.nuclearhack.stenographer.common.uikit.components.bar.StenoTopBar
import ru.nuclearhack.stenographer.common.uikit.components.overlay.StenoToastHost
import ru.nuclearhack.stenographer.common.uikit.components.overlay.StenoToastHostState
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

private enum class ScaffoldSlot { Top, Content, Bottom, Floating, Toast }

/**
 * Каркас экрана: фон, верхняя и нижняя панели, содержимое между ними. [floating] (мини-плеер) прижат к низу
 * содержимого, тосты показываются над ним. Панели сами учитывают системные отступы; в [content] приходит
 * [PaddingValues] с высотой [floating] снизу.
 */
@Composable
fun StenoScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    toastHostState: StenoToastHostState? = null,
    floating: @Composable BoxScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    SubcomposeLayout(modifier.fillMaxSize().background(StenoTheme.colors.bg)) { constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val top = subcompose(ScaffoldSlot.Top, topBar).map { it.measure(loose) }
        val bottom = subcompose(ScaffoldSlot.Bottom, bottomBar).map { it.measure(loose) }
        val topHeight = top.maxOfOrNull { it.height } ?: 0
        val bottomHeight = bottom.maxOfOrNull { it.height } ?: 0
        val bodyHeight = (height - topHeight - bottomHeight).coerceAtLeast(0)
        val bodyLoose = loose.copy(maxHeight = bodyHeight)

        val floatingItems = subcompose(ScaffoldSlot.Floating) {
            Box(Modifier.fillMaxWidth(), content = floating)
        }.map { it.measure(bodyLoose) }
        val floatingHeight = floatingItems.maxOfOrNull { it.height } ?: 0
        val toast = subcompose(ScaffoldSlot.Toast) {
            if (toastHostState != null) StenoToastHost(toastHostState)
        }.map { it.measure(bodyLoose) }
        val toastHeight = toast.maxOfOrNull { it.height } ?: 0
        val body = subcompose(ScaffoldSlot.Content) {
            content(PaddingValues(bottom = floatingHeight.toDp()))
        }.map { it.measure(Constraints.fixed(width, bodyHeight)) }

        val bodyBottom = topHeight + bodyHeight
        layout(width, height) {
            body.forEach { it.place(0, topHeight) }
            floatingItems.forEach { it.place(0, bodyBottom - floatingHeight) }
            toast.forEach { it.place(0, bodyBottom - floatingHeight - toastHeight - 8.dp.roundToPx()) }
            top.forEach { it.place(0, 0) }
            bottom.forEach { it.place(0, height - bottomHeight) }
        }
    }
}

@Composable
private fun ScaffoldPreview(dark: Boolean) {
    StenoThemedSurface(darkTheme = dark) {
        StenoScaffold(
            topBar = { StenoTopBar(title = "Главная", eyebrow = "12 января · 3 расшифровки") },
            bottomBar = {
                StenoBottomBar(
                    items = listOf(
                        StenoBottomBarItem("Главная", StenoIcons.Home),
                        StenoBottomBarItem("История", StenoIcons.History),
                        StenoBottomBarItem("Импорт", StenoIcons.Import),
                        StenoBottomBarItem("Настройки", StenoIcons.Settings),
                    ),
                    selectedIndex = 0,
                    onSelect = {},
                    onRecordClick = {},
                )
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).padding(20.dp)) { StenoText("Содержимое") }
        }
    }
}

@Preview(heightDp = 640)
@Composable
private fun StenoScaffoldLightPreview() = ScaffoldPreview(false)

@Preview(heightDp = 640)
@Composable
private fun StenoScaffoldDarkPreview() = ScaffoldPreview(true)
