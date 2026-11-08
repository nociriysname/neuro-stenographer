package ru.nuclearhack.stenographer.common.uikit.components.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.components.text.StenoIcon
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoThemedSurface

/** Плитка с иконкой на primarySoft: маркер строки списка или настройки. Декоративна (без описания). */
@Composable
fun StenoIconTile(
    icon: StenoIcons,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 13 / 44))
            .background(StenoTheme.colors.primarySoft),
        contentAlignment = Alignment.Center,
    ) {
        StenoIcon(icon, contentDescription = null, tint = StenoTheme.colors.primaryInk)
    }
}

@Preview
@Composable
private fun StenoIconTileLightPreview() = StenoThemedSurface(darkTheme = false) { StenoIconTile(StenoIcons.Mic) }

@Preview
@Composable
private fun StenoIconTileDarkPreview() = StenoThemedSurface(darkTheme = true) { StenoIconTile(StenoIcons.Mic) }
