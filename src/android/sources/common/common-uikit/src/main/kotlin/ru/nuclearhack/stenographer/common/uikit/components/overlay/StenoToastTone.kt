package ru.nuclearhack.stenographer.common.uikit.components.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme

/** Тон тоста: определяет цвет иконки. */
enum class StenoToastTone { Info, Success, Error }

/** Цвет иконки тоста из макета S12: тост светлой темы тёмный, и наоборот, поэтому цвета инвертированы. */
@Composable
@ReadOnlyComposable
internal fun StenoToastTone.iconColor(): Color {
    val dark = StenoTheme.colors.isDark
    return when (this) {
        StenoToastTone.Success -> if (dark) Color(0xFF17703F) else Color(0xFF7BE0A8)
        StenoToastTone.Error -> if (dark) Color(0xFFB02F2F) else Color(0xFFFF9C93)
        StenoToastTone.Info -> if (dark) Color(0xFF0F5C98) else Color(0xFF8CCBFB)
    }
}
