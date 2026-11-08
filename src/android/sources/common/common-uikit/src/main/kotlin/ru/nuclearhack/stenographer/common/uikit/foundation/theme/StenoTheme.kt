package ru.nuclearhack.stenographer.common.uikit.foundation.theme

import android.provider.Settings
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

private val LocalStenoColors = staticCompositionLocalOf { LightStenoColors }
private val LocalStenoTypography = staticCompositionLocalOf { DefaultStenoTypography }
private val LocalStenoSpacing = staticCompositionLocalOf { StenoSpacing() }
private val LocalStenoShapes = staticCompositionLocalOf { StenoShapes() }
private val LocalStenoElevation = staticCompositionLocalOf { LightStenoElevation }
private val LocalStenoMotion = staticCompositionLocalOf { StenoMotion(reduced = false) }

/**
 * Тема «Бумага и небо» на собственных токенах, без MaterialTheme: экраны собираются из компонентов common-uikit.
 * Из Material 3 берётся только ripple — отклик на нажатие.
 */
@Composable
fun StenoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkStenoColors else LightStenoColors
    val context = LocalContext.current
    val motion = remember(context) {
        val scale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        StenoMotion(reduced = scale == 0f)
    }
    CompositionLocalProvider(
        LocalStenoColors provides colors,
        LocalStenoTypography provides DefaultStenoTypography,
        LocalStenoElevation provides if (darkTheme) DarkStenoElevation else LightStenoElevation,
        LocalStenoMotion provides motion,
        LocalIndication provides ripple(color = colors.primary),
        LocalTextSelectionColors provides TextSelectionColors(
            handleColor = colors.primary,
            backgroundColor = colors.primarySoft,
        ),
        content = content,
    )
}

/** Доступ к теме: `StenoTheme.colors.primary`, `StenoTheme.spacing.s4`, `StenoTheme.shapes.md`. */
object StenoTheme {
    val colors: StenoColors
        @Composable @ReadOnlyComposable
        get() = LocalStenoColors.current

    val typography: StenoTypography
        @Composable @ReadOnlyComposable
        get() = LocalStenoTypography.current

    val spacing: StenoSpacing
        @Composable @ReadOnlyComposable
        get() = LocalStenoSpacing.current

    val shapes: StenoShapes
        @Composable @ReadOnlyComposable
        get() = LocalStenoShapes.current

    val elevation: StenoElevation
        @Composable @ReadOnlyComposable
        get() = LocalStenoElevation.current

    val motion: StenoMotion
        @Composable @ReadOnlyComposable
        get() = LocalStenoMotion.current
}

/** Тема + фон приложения: для превью и корня экрана. */
@Composable
fun StenoThemedSurface(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    StenoTheme(darkTheme) {
        Box(Modifier.background(StenoTheme.colors.bg)) { content() }
    }
}
