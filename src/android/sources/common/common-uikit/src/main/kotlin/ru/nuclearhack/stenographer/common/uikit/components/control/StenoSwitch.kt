package ru.nuclearhack.stenographer.common.uikit.components.control

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.components.modifier.stenoFocusRing
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/**
 * Переключатель 52×30 с квадратным «ползунком». Название настройки подписывает соседний текст:
 * кладите переключатель и подпись в один кликабельный ряд либо задавайте семантику снаружи.
 */
@Composable
fun StenoSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = StenoTheme.colors
    val motion = StenoTheme.motion
    val shape = RoundedCornerShape(10.dp)
    val thumbShape = RoundedCornerShape(7.dp)
    val interaction = remember { MutableInteractionSource() }
    val track by animateColorAsState(if (checked) colors.primary else colors.surface3, motion.base(), "switchTrack")
    val trackBorder by animateColorAsState(if (checked) colors.primary else colors.lineStrong, motion.base(), "border")
    val thumb by animateColorAsState(if (checked) colors.onPrimary else colors.switchThumb, motion.base(), "thumb")
    val thumbX by animateDpAsState(if (checked) 26.dp else 4.dp, motion.base(), "switchThumbX")

    Box(
        modifier = modifier
            .stenoFocusRing(shape)
            .sizeIn(minWidth = 52.dp, minHeight = 48.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .toggleable(
                value = checked,
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 30.dp)
                .background(track, shape)
                .border(1.dp, trackBorder, shape),
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbX, y = 4.dp)
                    .size(22.dp)
                    .dropShadow(thumbShape, StenoTheme.elevation.card)
                    .background(thumb, thumbShape),
            )
        }
    }
}

@Composable
private fun SwitchPreviewContent() {
    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StenoSwitch(checked = false, onCheckedChange = {})
        StenoSwitch(checked = true, onCheckedChange = {})
        StenoSwitch(checked = false, onCheckedChange = {}, enabled = false)
        StenoSwitch(checked = true, onCheckedChange = {}, enabled = false)
    }
}

@Preview(showBackground = true)
@Composable
private fun StenoSwitchLightPreview() {
    StenoThemedSurface(darkTheme = false) { SwitchPreviewContent() }
}

@Preview(showBackground = true)
@Composable
private fun StenoSwitchDarkPreview() {
    StenoThemedSurface(darkTheme = true) { SwitchPreviewContent() }
}
