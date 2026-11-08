package ru.nuclearhack.stenographer.feature.transcript.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme
import ru.nuclearhack.stenographer.feature.transcript.presentation.logic.SearchMatch

/** Совпадения одной реплики: глобальный номер совпадения (для «текущего») и само совпадение. */
internal typealias UtteranceMatches = List<IndexedValue<SearchMatch>>

/** Текст реплики с подсветкой совпадений поиска: текущее — на accent, остальные — на accentSoft. */
@Composable
internal fun highlightedText(text: String, matches: UtteranceMatches?, currentMatch: Int): AnnotatedString {
    val colors = StenoTheme.colors
    return remember(text, matches, currentMatch, colors) {
        if (matches.isNullOrEmpty()) return@remember AnnotatedString(text)
        buildAnnotatedString {
            var cursor = 0
            matches.forEach { (number, match) ->
                append(text.substring(cursor, match.start))
                val style = if (number == currentMatch) {
                    SpanStyle(background = colors.accent, color = colors.onPrimary)
                } else {
                    SpanStyle(background = colors.accentSoft, color = colors.accentInk)
                }
                withStyle(style) { append(text.substring(match.start, match.end)) }
                cursor = match.end
            }
            append(text.substring(cursor))
        }
    }
}
