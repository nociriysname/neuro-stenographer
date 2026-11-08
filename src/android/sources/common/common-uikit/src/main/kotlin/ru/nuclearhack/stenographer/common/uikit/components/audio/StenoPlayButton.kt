package ru.nuclearhack.stenographer.common.uikit.components.audio

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoIcon
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/** Главная кнопка плеера: 56 dp, скругление 18, primary, мягкая тень; «Играть» / «Пауза». */
@Composable
fun StenoPlayButton(
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = StenoTheme.colors
    val shape = StenoTheme.shapes.lg
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val description = if (playing) "Пауза" else "Играть"
    Box(
        modifier
            .size(56.dp)
            .dropShadow(
                shape,
                Shadow(radius = 10.dp, color = colors.primary.copy(alpha = 0.3f), offset = DpOffset(0.dp, 3.dp)),
            )
            .clip(shape)
            .background(if (pressed) colors.primaryPress else colors.primary)
            .clickable(
                interactionSource = source,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        StenoIcon(
            icon = if (playing) StenoIcons.Pause else StenoIcons.Play,
            contentDescription = null,
            tint = colors.onPrimary,
            size = 32.dp,
        )
    }
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun StenoPlayButtonLightPreview() = StenoPlayButtonPreview(darkTheme = false)

@Preview(name = "Dark", showBackground = true)
@Composable
private fun StenoPlayButtonDarkPreview() = StenoPlayButtonPreview(darkTheme = true)

@Composable
private fun StenoPlayButtonPreview(darkTheme: Boolean) {
    StenoThemedSurface(darkTheme) {
        Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StenoPlayButton(playing = false, onClick = ::previewNoop)
            StenoPlayButton(playing = true, onClick = ::previewNoop)
        }
    }
}
