package ru.nuclearhack.stenographer.common.uikit.components.text

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.nuclearhack.stenographer.common.uikit.foundation.icon.StenoIcons
import ru.nuclearhack.stenographer.common.uikit.foundation.theme.StenoTheme

/** Текст в стиле темы: по умолчанию основной текст на фоне. */
@Composable
fun StenoText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = StenoTheme.typography.body,
    color: Color = StenoTheme.colors.text,
    textAlign: TextAlign = TextAlign.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color, textAlign = textAlign),
        maxLines = maxLines,
        overflow = overflow,
    )
}

/** Размеченный текст: подсветка слова при проигрывании, совпадения поиска. */
@Composable
fun StenoText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = StenoTheme.typography.body,
    color: Color = StenoTheme.colors.text,
    textAlign: TextAlign = TextAlign.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.copy(color = color, textAlign = textAlign),
        maxLines = maxLines,
        overflow = overflow,
    )
}

/** Иконка из набора [StenoIcons], окрашенная в [tint]. Размеры из макета: 20 (s), 24, 32 (l), 40 (xl). */
@Composable
fun StenoIcon(
    icon: StenoIcons,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = StenoTheme.colors.text,
    size: Dp = 24.dp,
) {
    StenoIcon(icon.painter, contentDescription, modifier, tint, size)
}

@Composable
fun StenoIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = StenoTheme.colors.text,
    size: Dp = 24.dp,
) {
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = ColorFilter.tint(tint),
    )
}
