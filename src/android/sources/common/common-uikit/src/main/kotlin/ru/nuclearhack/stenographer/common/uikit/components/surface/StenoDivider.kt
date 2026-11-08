package ru.nuclearhack.stenographer.common.uikit.components.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme

/** Разделитель 1 dp цвета line. */
@Composable
fun StenoDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(StenoTheme.colors.line))
}
