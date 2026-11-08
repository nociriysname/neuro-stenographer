package ru.nuclearhack.stenographer.common.uikit.components.overlay

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoIconButton
import ru.nuclearhack.stenographer.common.uikit.components.button.StenoIconButtonVariant
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoText
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/**
 * Модальный шит снизу на весь экран (своё окно): scrim гаснет за [motion.base], шит выезжает за slow,
 * закрывается тапом по scrim, кнопкой «Закрыть» (если есть [title]), кнопкой «назад» и свайпом вниз.
 */
@Composable
fun StenoBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val motion = StenoTheme.motion
    val scope = rememberCoroutineScope()
    var dragPx by remember { mutableFloatStateOf(0f) }
    var sheetHeightPx by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(visible) { if (visible) dragPx = 0f }
    val draggable = rememberDraggableState { delta -> dragPx = (dragPx + delta).coerceAtLeast(0f) }

    StenoOverlayWindow(visible, onDismissRequest, slowBody = true) { scrim, body ->
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = scrim() }
                    .background(StenoTheme.colors.scrim)
                    .pointerInput(Unit) { detectTapGestures { onDismissRequest() } },
            )
            StenoBottomSheetContent(
                title = title,
                onClose = onDismissRequest,
                modifier = modifier
                    .align(Alignment.BottomCenter)
                    .heightIn(max = maxHeight * 0.92f)
                    .graphicsLayer {
                        sheetHeightPx = size.height
                        translationY = (1f - body()) * size.height + dragPx
                    }
                    .draggable(
                        state = draggable,
                        orientation = Orientation.Vertical,
                        onDragStopped = { velocity ->
                            if (dragPx > sheetHeightPx * 0.3f || velocity > 1_200f) {
                                onDismissRequest()
                            } else {
                                scope.launch {
                                    animate(dragPx, 0f, animationSpec = motion.base()) { value, _ -> dragPx = value }
                                }
                            }
                        },
                    )
                    .pointerInput(Unit) { detectTapGestures { } },
                content = content,
            )
        }
    }
}

/** Статичное тело шита без окна и анимаций: поверхность, ручка, заголовок, содержимое. */
@Composable
internal fun StenoBottomSheetContent(
    title: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = StenoTheme.colors
    val shape = StenoTheme.shapes.sheet
    Column(
        modifier = modifier
            .fillMaxWidth()
            .dropShadow(shape, StenoTheme.elevation.sheet)
            .clip(shape)
            .background(colors.bg)
            .border(1.dp, colors.line, shape)
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp)
            .semantics { paneTitle = title.orEmpty() },
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 40.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.lineStrong),
        )
        if (title != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StenoText(
                    title,
                    modifier = Modifier.weight(1f),
                    style = StenoTheme.typography.subtitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                StenoIconButton(
                    icon = StenoIcons.Close,
                    contentDescription = "Закрыть",
                    onClick = onClose,
                    variant = StenoIconButtonVariant.Ghost,
                )
            }
        }
        content()
    }
}

@Composable
private fun SheetPreview(dark: Boolean) {
    StenoThemedSurface(darkTheme = dark) {
        StenoBottomSheetContent(title = "Кто говорит?", onClose = {}) {
            StenoText("Выберите имя для спикера 1", color = StenoTheme.colors.text2)
        }
    }
}

@Preview
@Composable
private fun StenoBottomSheetLightPreview() = SheetPreview(false)

@Preview
@Composable
private fun StenoBottomSheetDarkPreview() = SheetPreview(true)
