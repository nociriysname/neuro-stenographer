package ru.nuclearhack.stenographer.common.uikit.components.field

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme

/**
 * Общая рамка полей ввода (.fld): тонкий контур, при фокусе — контур primary и ореол 3 dp цвета primaryMid.
 */
@Composable
internal fun StenoFieldContainer(
    focused: Boolean,
    modifier: Modifier = Modifier,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = StenoTheme.colors
    val shape = StenoTheme.shapes.md
    val borderColor by animateColorAsState(
        targetValue = if (focused) colors.primary else colors.lineStrong,
        animationSpec = StenoTheme.motion.fast(),
        label = "fieldBorder",
    )
    val haloColor by animateColorAsState(
        targetValue = if (focused) colors.primaryMid.copy(alpha = 0.3f) else colors.primaryMid.copy(alpha = 0f),
        animationSpec = StenoTheme.motion.fast(),
        label = "fieldHalo",
    )
    Row(
        modifier = modifier
            .drawBehind {
                val halo = 3.dp.toPx()
                val grow = halo / 2
                val outline = shape.createOutline(Size(size.width + halo, size.height + halo), layoutDirection, this)
                translate(-grow, -grow) { drawOutline(outline, haloColor, style = Stroke(halo)) }
            }
            .background(colors.surface1, shape)
            .border(1.dp, borderColor, shape)
            .heightIn(min = 48.dp)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = verticalAlignment,
        content = content,
    )
}
