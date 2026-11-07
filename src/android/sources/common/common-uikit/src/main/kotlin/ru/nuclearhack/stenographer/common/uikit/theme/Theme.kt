package ru.nuclearhack.stenographer.common.uikit.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun StenographerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

/** Цвета говорящих. Nemotron-3-Diarization различает до 8 спикеров — по цвету на каждого. */
object SpeakerPalette {
    private val colors = listOf(
        Color(0xFF4F7BF7),
        Color(0xFFE5793B),
        Color(0xFF2EA66B),
        Color(0xFFB04FD8),
        Color(0xFFD94A6A),
        Color(0xFF1FA2B5),
        Color(0xFF8A6D3B),
        Color(0xFF6B7280),
    )

    fun colorFor(speakerIndex: Int): Color = colors[speakerIndex.mod(colors.size)]
}
