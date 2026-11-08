package ru.nuclearhack.stenographer.common.uikit.foundation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Цветовые роли дизайн-системы «Бумага и небо» (docs/design/claude-design/tokens.json).
 * Роли одинаковы в обеих темах, меняются только значения. Пары «текст на фоне» проходят WCAG AA.
 */
@Immutable
data class StenoColors(
    val isDark: Boolean,
    /** Фон экрана и «бумага» карточек. */
    val bg: Color,
    /** Тонкие подложки: поле поиска, плоская карточка, мини-карта. */
    val surface1: Color,
    /** Трек прогресса, сегменты, нажатое состояние. */
    val surface2: Color,
    /** Выключенный переключатель, пустые столбики уровня. */
    val surface3: Color,
    val line: Color,
    val lineStrong: Color,
    val text: Color,
    val text2: Color,
    /** Подсказки и placeholder: всё ещё AA на bg. */
    val text3: Color,
    val primary: Color,
    val primaryPress: Color,
    val onPrimary: Color,
    val primarySoft: Color,
    /** Текст и иконки цвета primary на светлых подложках (ссылки, активная вкладка). */
    val primaryInk: Color,
    /** Ореол фокуса и графика (волна). */
    val primaryMid: Color,
    /** Тёплый акцент: совпадения поиска и детали. */
    val accent: Color,
    val accentSoft: Color,
    val accentInk: Color,
    val success: Color,
    val successSoft: Color,
    val warning: Color,
    val warningInk: Color,
    val warningSoft: Color,
    val error: Color,
    val errorInk: Color,
    val errorSoft: Color,
    val onError: Color,
    val rec: Color,
    val recInk: Color,
    val recSoft: Color,
    val toast: Color,
    val onToast: Color,
    val toastAction: Color,
    val scrim: Color,
    val switchThumb: Color,
    /** Цвета говорящих: Nemotron различает до 8 спикеров. */
    val speakers: List<Color>,
    /** Цифра или инициал на цвете спикера. */
    val onSpeaker: Color,
    /** Подсветка эмоций в порядке [EmotionTone.entries]. */
    val emotions: List<EmotionColors>,
) {
    fun speaker(index: Int): Color = speakers[index.mod(speakers.size)]

    fun emotion(tone: EmotionTone): EmotionColors = emotions[tone.ordinal]
}

/** Эмоция реплики в интерфейсе: фон реплики и цвет маркера на ленте. */
enum class EmotionTone { Neutral, Joy, Irritation, Sadness, Surprise }

@Immutable
data class EmotionColors(val background: Color, val mark: Color)

val LightStenoColors = StenoColors(
    isDark = false,
    bg = Color(0xFFFFFFFF),
    surface1 = Color(0xFFF6F9FC),
    surface2 = Color(0xFFEDF2F7),
    surface3 = Color(0xFFDCE5EE),
    line = Color(0xFFE1E8EF),
    lineStrong = Color(0xFFC3CFDB),
    text = Color(0xFF14202B),
    text2 = Color(0xFF4A5B6B),
    text3 = Color(0xFF5F7081),
    primary = Color(0xFF1370BA),
    primaryPress = Color(0xFF0F5C98),
    onPrimary = Color(0xFFFFFFFF),
    primarySoft = Color(0xFFE6F3FD),
    primaryInk = Color(0xFF0F5C98),
    primaryMid = Color(0xFF2A94E2),
    accent = Color(0xFFE58A00),
    accentSoft = Color(0xFFFFF0DF),
    accentInk = Color(0xFF9A4E00),
    success = Color(0xFF17703F),
    successSoft = Color(0xFFE3F5EA),
    warning = Color(0xFFE8A317),
    warningInk = Color(0xFF8A5300),
    warningSoft = Color(0xFFFFF2D6),
    error = Color(0xFFC73A3A),
    errorInk = Color(0xFFB02F2F),
    errorSoft = Color(0xFFFDE8E6),
    onError = Color(0xFFFFFFFF),
    rec = Color(0xFFD92D25),
    recInk = Color(0xFFB3231C),
    recSoft = Color(0xFFFDE9E7),
    toast = Color(0xFF1B2B3A),
    onToast = Color(0xFFFFFFFF),
    toastAction = Color(0xFF8CCBFB),
    scrim = Color(0x730E1A26),
    switchThumb = Color(0xFFFFFFFF),
    speakers = listOf(
        Color(0xFF1370BA),
        Color(0xFFC4421F),
        Color(0xFF1E7F45),
        Color(0xFF7448CC),
        Color(0xFF9A5B00),
        Color(0xFFBF2F78),
        Color(0xFF0A7A70),
        Color(0xFF566677),
    ),
    onSpeaker = Color(0xFFFFFFFF),
    emotions = listOf(
        EmotionColors(background = Color(0xFFEEF2F6), mark = Color(0xFF7C8CA0)),
        EmotionColors(background = Color(0xFFFFF1C7), mark = Color(0xFFD99A00)),
        EmotionColors(background = Color(0xFFFFE2D8), mark = Color(0xFFD9532F)),
        EmotionColors(background = Color(0xFFE5E8FB), mark = Color(0xFF6A6FD6)),
        EmotionColors(background = Color(0xFFDDF3EC), mark = Color(0xFF2AA58A)),
    ),
)

val DarkStenoColors = StenoColors(
    isDark = true,
    bg = Color(0xFF121A24),
    surface1 = Color(0xFF18222E),
    surface2 = Color(0xFF202C3A),
    surface3 = Color(0xFF2C3A4A),
    line = Color(0xFF263444),
    lineStrong = Color(0xFF3A4B5E),
    text = Color(0xFFE8EEF4),
    text2 = Color(0xFFA9B8C7),
    text3 = Color(0xFF8FA1B3),
    primary = Color(0xFF5DB6F5),
    primaryPress = Color(0xFF79C4F8),
    onPrimary = Color(0xFF07243D),
    primarySoft = Color(0xFF17304A),
    primaryInk = Color(0xFF7CC4F8),
    primaryMid = Color(0xFF4FADEF),
    accent = Color(0xFFF2A33A),
    accentSoft = Color(0xFF3A2B17),
    accentInk = Color(0xFFF6B866),
    success = Color(0xFF5BD18C),
    successSoft = Color(0xFF173326),
    warning = Color(0xFFF2B84B),
    warningInk = Color(0xFFF2B84B),
    warningSoft = Color(0xFF3A3017),
    error = Color(0xFFFF8A80),
    errorInk = Color(0xFFFF9C93),
    errorSoft = Color(0xFF3D2223),
    onError = Color(0xFF2A0A0A),
    rec = Color(0xFFFF5A4F),
    recInk = Color(0xFFFF7A70),
    recSoft = Color(0xFF40201F),
    toast = Color(0xFFE8EEF4),
    onToast = Color(0xFF0F1A24),
    toastAction = Color(0xFF0F5C98),
    scrim = Color(0xA803080E),
    switchThumb = Color(0xFFC9D5E0),
    speakers = listOf(
        Color(0xFF5DB6F5),
        Color(0xFFFF8A6B),
        Color(0xFF54D08A),
        Color(0xFFB596FF),
        Color(0xFFF2B84B),
        Color(0xFFF27FB2),
        Color(0xFF4DD4C4),
        Color(0xFFA7B4C2),
    ),
    onSpeaker = Color(0xFF0B1620),
    emotions = listOf(
        EmotionColors(background = Color(0xFF222F3E), mark = Color(0xFF8FA1B3)),
        EmotionColors(background = Color(0xFF3A3418), mark = Color(0xFFF2C14B)),
        EmotionColors(background = Color(0xFF432823), mark = Color(0xFFFF8A6B)),
        EmotionColors(background = Color(0xFF272C4C), mark = Color(0xFF9AA2FF)),
        EmotionColors(background = Color(0xFF1C3A35), mark = Color(0xFF4DD4B8)),
    ),
)
