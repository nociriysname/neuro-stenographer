package ru.nuclearhack.stenographer.common.uikit.foundation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ru.nuclearhack.stenographer.common.uikit.R

/**
 * Golos Text (ParaType, OFL-1.1) — гротеск, нарисованный под кириллицу. Вшит в APK как переменный шрифт
 * (ось wght 400–900): интернета у приложения нет, загружаемые шрифты не годятся.
 */
val GolosText = FontFamily(
    golos(FontWeight.Normal),
    golos(FontWeight.Medium),
    golos(FontWeight.SemiBold),
    golos(FontWeight.Bold),
)

private fun golos(weight: FontWeight) = Font(
    resId = R.font.golos_text,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** Шкала текста из макета: размер / высота строки / начертание. Всё в sp — растёт с системным шрифтом. */
@Immutable
data class StenoTypography(
    /** 36/42 · 700 — онбординг, пустые состояния. */
    val display: TextStyle,
    /** 24/30 · 700 — заголовок экрана. */
    val title: TextStyle,
    /** 18/24 · 600 — секция, шит, диалог. */
    val subtitle: TextStyle,
    /** 16/24 · 400 — основной текст интерфейса. */
    val body: TextStyle,
    /** 17/28 · 400 — реплики расшифровки, длинное чтение. */
    val transcript: TextStyle,
    /** 14/20 · 400 — подписи, вторичные строки. */
    val bodySmall: TextStyle,
    /** 14/20 · 600 — названия полей, сегменты, чипы. */
    val label: TextStyle,
    /** 16/20 · 600 — кнопки, заголовки строк списка. */
    val labelLarge: TextStyle,
    /** 12/16 · 500 — таймкоды, метки; мельче не бывает. */
    val caption: TextStyle,
    /** 12/16 · 600, капс — «глазок» над заголовком, подписи групп в настройках. */
    val overline: TextStyle,
    /** 64/68 · 600, табличные цифры — таймер записи. */
    val timer: TextStyle,
)

/** Табличные цифры: таймкоды и проценты не «прыгают» по ширине. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

private fun golosStyle(size: Int, lineHeight: Int, weight: FontWeight, tracking: TextUnit = TextUnit.Unspecified) =
    TextStyle(
        fontFamily = GolosText,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = tracking,
        hyphens = Hyphens.Auto,
        lineBreak = LineBreak.Paragraph,
    )

val DefaultStenoTypography = StenoTypography(
    display = golosStyle(36, 42, FontWeight.Bold, (-0.01).em),
    title = golosStyle(24, 30, FontWeight.Bold, (-0.005).em),
    subtitle = golosStyle(18, 24, FontWeight.SemiBold),
    body = golosStyle(16, 24, FontWeight.Normal),
    transcript = golosStyle(17, 28, FontWeight.Normal),
    bodySmall = golosStyle(14, 20, FontWeight.Normal),
    label = golosStyle(14, 20, FontWeight.SemiBold),
    labelLarge = golosStyle(16, 20, FontWeight.SemiBold),
    caption = golosStyle(12, 16, FontWeight.Medium, 0.01.em),
    overline = golosStyle(12, 16, FontWeight.SemiBold, 0.04.em),
    timer = golosStyle(64, 68, FontWeight.SemiBold, (-0.01).em).tabular(),
)
