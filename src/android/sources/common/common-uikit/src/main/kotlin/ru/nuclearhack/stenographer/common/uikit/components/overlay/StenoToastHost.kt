package ru.nuclearhack.stenographer.common.uikit.components.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme

/** Место показа тостов: выезжает снизу с затуханием. Ставится над нижней панелью (это делает StenoScaffold). */
@Composable
fun StenoToastHost(state: StenoToastHostState, modifier: Modifier = Modifier) {
    val motion = StenoTheme.motion
    AnimatedContent(
        targetState = state.current,
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        contentAlignment = Alignment.BottomCenter,
        transitionSpec = {
            (fadeIn(motion.base()) + slideInVertically(motion.base()) { it / 2 })
                .togetherWith(fadeOut(motion.fast()) + slideOutVertically(motion.fast()) { it / 2 })
                .using(SizeTransform(clip = false))
        },
        label = "toast",
    ) { data ->
        if (data != null) {
            StenoToast(
                message = data.message,
                icon = data.icon,
                iconTint = data.tone.iconColor(),
                actionLabel = data.actionLabel,
                onAction = data::performAction,
            )
        }
    }
}
