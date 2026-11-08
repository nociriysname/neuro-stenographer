package ru.nuclearhack.stenographer.common.uikit.format

import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

/** Таймкод как в макете: `12:04` для записей короче часа, `1:02:05` — для длинных. */
fun formatTimestamp(ms: Long): String =
    ms.coerceAtLeast(0L).milliseconds.toComponents { hours, minutes, seconds, _ ->
        if (hours > 0) {
            String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
        }
    }
