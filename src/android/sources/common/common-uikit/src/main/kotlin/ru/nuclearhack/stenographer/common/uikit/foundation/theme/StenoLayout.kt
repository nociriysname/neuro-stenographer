package ru.nuclearhack.stenographer.common.uikit.foundation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * Отступы, сетка 4 dp: имя шага = число четвёрок (s4 = 16 dp), как в tokens.json.
 * Это общая шкала для экранов; размеры конкретных компонентов живут внутри самих компонентов.
 */
@Immutable
data class StenoSpacing(
    val s1: Dp = 4.dp,
    val s2: Dp = 8.dp,
    val s3: Dp = 12.dp,
    val s4: Dp = 16.dp,
    val s5: Dp = 20.dp,
    val s6: Dp = 24.dp,
    val s8: Dp = 32.dp,
    val s10: Dp = 40.dp,
    val s12: Dp = 48.dp,
    val s16: Dp = 64.dp,
    /** Боковые поля экрана: на 360 dp остаётся 320 dp контента. */
    val screen: Dp = 20.dp,
)

/** Радиусы — своя геометрия без «пилюль». */
@Immutable
data class StenoShapes(
    val xs: RoundedCornerShape = RoundedCornerShape(6.dp),
    val sm: RoundedCornerShape = RoundedCornerShape(10.dp),
    /** Кнопки, поля, иконочные кнопки. */
    val md: RoundedCornerShape = RoundedCornerShape(14.dp),
    /** Карточки. */
    val lg: RoundedCornerShape = RoundedCornerShape(18.dp),
    /** Диалоги, большие карточки действий. */
    val xl: RoundedCornerShape = RoundedCornerShape(22.dp),
    val sheet: RoundedCornerShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
)

/** Мягкие тени вместо тональных подложек Material: карточка, всплывающее (диалог, тост), шит. */
@Immutable
data class StenoElevation(val card: Shadow, val popup: Shadow, val sheet: Shadow)

val LightStenoElevation = StenoElevation(
    card = Shadow(radius = 2.dp, color = Color(0x0F14202B), offset = DpOffset(0.dp, 1.dp)),
    popup = Shadow(radius = 20.dp, color = Color(0x1A14202B), offset = DpOffset(0.dp, 6.dp)),
    sheet = Shadow(radius = 36.dp, color = Color(0x2E14202B), offset = DpOffset(0.dp, (-10).dp)),
)

val DarkStenoElevation = StenoElevation(
    card = Shadow(radius = 2.dp, color = Color(0x59000000), offset = DpOffset(0.dp, 1.dp)),
    popup = Shadow(radius = 24.dp, color = Color(0x73000000), offset = DpOffset(0.dp, 8.dp)),
    sheet = Shadow(radius = 36.dp, color = Color(0x8C000000), offset = DpOffset(0.dp, (-10).dp)),
)
