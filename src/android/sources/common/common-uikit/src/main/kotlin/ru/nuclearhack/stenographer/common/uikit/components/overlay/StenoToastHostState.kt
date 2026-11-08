package ru.nuclearhack.stenographer.common.uikit.components.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import kotlin.coroutines.resume

/** Тост в очереди [StenoToastHostState]: то, что рисует [StenoToastHost]. */
@Stable
class StenoToastData internal constructor(
    val message: String,
    val icon: StenoIcons?,
    val tone: StenoToastTone,
    val actionLabel: String?,
    private val continuation: CancellableContinuation<StenoToastResult>,
) {
    fun performAction() {
        if (continuation.isActive) continuation.resume(StenoToastResult.ActionPerformed)
    }

    fun dismiss() {
        if (continuation.isActive) continuation.resume(StenoToastResult.Dismissed)
    }
}

/**
 * Очередь тостов: [show] приостанавливается до закрытия тоста (действие, смахивание или таймаут [durationMillis]);
 * следующие ждут своей очереди.
 */
@Stable
class StenoToastHostState(private val durationMillis: Long) {
    private val mutex = Mutex()

    /** Тост, показываемый сейчас. */
    var current by mutableStateOf<StenoToastData?>(null)
        private set

    suspend fun show(
        message: String,
        icon: StenoIcons? = null,
        tone: StenoToastTone = StenoToastTone.Info,
        actionLabel: String? = null,
    ): StenoToastResult = mutex.withLock {
        try {
            withTimeoutOrNull(durationMillis) {
                suspendCancellableCoroutine { continuation ->
                    current = StenoToastData(message, icon, tone, actionLabel, continuation)
                }
            } ?: StenoToastResult.Dismissed
        } finally {
            current = null
        }
    }
}

/** Состояние очереди тостов с длительностью из темы. */
@Composable
fun rememberStenoToastHostState(): StenoToastHostState {
    val millis = StenoTheme.motion.toastMillis
    return remember(millis) { StenoToastHostState(millis) }
}
