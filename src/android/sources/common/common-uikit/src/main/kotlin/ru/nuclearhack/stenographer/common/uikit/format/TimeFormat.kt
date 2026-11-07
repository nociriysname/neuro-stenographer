package ru.nuclearhack.stenographer.common.uikit.format

import java.util.Locale

/** `02:05.3` для записей короче часа, `1:02:05` — для длинных. */
fun formatTimestamp(ms: Long): String {
    val totalSeconds = ms / 1_000
    val hours = totalSeconds / 3_600
    val minutes = totalSeconds / 60 % 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%02d:%02d.%d", minutes, seconds, ms % 1_000 / 100)
    }
}
