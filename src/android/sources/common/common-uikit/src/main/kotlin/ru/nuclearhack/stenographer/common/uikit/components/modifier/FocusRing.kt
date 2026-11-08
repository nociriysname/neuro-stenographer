package ru.nuclearhack.stenographer.common.uikit.components.modifier

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme

/**
 * Кольцо фокуса из макета (.foc): обводка 3 dp цвета primaryMid с прозрачностью 60 % на расстоянии 2 dp от контрола.
 * Ставится в цепочке раньше `clickable`/`focusable` и раньше `clip`, иначе кольцо обрежется.
 */
@Composable
fun Modifier.stenoFocusRing(shape: Shape): Modifier {
    var focused by remember { mutableStateOf(false) }
    val color = StenoTheme.colors.primaryMid.copy(alpha = 0.6f)
    return this
        .onFocusChanged { focused = it.isFocused }
        .drawBehind {
            if (focused) {
                val stroke = 3.dp.toPx()
                // Центр штриха отстоит от контура на offset + половину толщины.
                val grow = 2.dp.toPx() + stroke / 2
                val outline = shape.createOutline(
                    Size(size.width + grow * 2, size.height + grow * 2),
                    layoutDirection,
                    this,
                )
                translate(-grow, -grow) { drawOutline(outline, color, style = Stroke(stroke)) }
            }
        }
}
