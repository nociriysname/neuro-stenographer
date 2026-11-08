package ru.nuclearhack.stenographer.common.uikit.components.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme

/**
 * Окно-оверлей на весь экран для шита и диалога: свои анимации вместо платформенных, без системного затемнения.
 * [content] получает прогресс появления scrim и тела (0..1) и остаётся в композиции, пока идёт анимация ухода.
 */
@Composable
internal fun StenoOverlayWindow(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    slowBody: Boolean,
    content: @Composable (scrim: () -> Float, body: () -> Float) -> Unit,
) {
    val motion = StenoTheme.motion
    val scrim = remember { Animatable(0f) }
    val body = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        val target = if (visible) 1f else 0f
        coroutineScope {
            launch { scrim.animateTo(target, motion.base()) }
            launch { body.animateTo(target, if (slowBody) motion.slow() else motion.base()) }
        }
    }
    if (visible || scrim.value > 0f || body.value > 0f) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            val window = (LocalView.current.parent as? DialogWindowProvider)?.window
            SideEffect {
                window?.setDimAmount(0f)
                window?.setWindowAnimations(-1)
            }
            Box(Modifier.fillMaxSize()) { content({ scrim.value }, { body.value }) }
        }
    }
}
