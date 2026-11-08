package ru.nuclearhack.stenographer.common.uikit.foundation.icon

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import ru.nuclearhack.stenographer.common.uikit.R

/**
 * Собственный набор иконок из макета: сетка 24, линия 1.75, скруглённые концы. Красятся в цвет текста
 * компонентом StenoIcon. Источник — docs/design/claude-design/icons, xml сгенерированы из svg.
 */
enum class StenoIcons(@param:DrawableRes val resId: Int) {
    AudioFile(R.drawable.ic_steno_audiofile),
    Back(R.drawable.ic_steno_back),
    Check(R.drawable.ic_steno_check),
    Chevron(R.drawable.ic_steno_chevron),
    Chip(R.drawable.ic_steno_chip),
    Close(R.drawable.ic_steno_close),
    Copy(R.drawable.ic_steno_copy),
    Delete(R.drawable.ic_steno_delete),
    Edit(R.drawable.ic_steno_edit),
    History(R.drawable.ic_steno_history),
    Home(R.drawable.ic_steno_home),
    Import(R.drawable.ic_steno_import),
    Info(R.drawable.ic_steno_info),
    Mic(R.drawable.ic_steno_mic),
    More(R.drawable.ic_steno_more),
    NoCloud(R.drawable.ic_steno_no_cloud),
    Pause(R.drawable.ic_steno_pause),
    Play(R.drawable.ic_steno_play),
    Protocol(R.drawable.ic_steno_protocol),
    Record(R.drawable.ic_steno_record),
    Retry(R.drawable.ic_steno_retry),
    Search(R.drawable.ic_steno_search),
    Settings(R.drawable.ic_steno_settings),
    Share(R.drawable.ic_steno_share),
    Shield(R.drawable.ic_steno_shield),
    SkipSilence(R.drawable.ic_steno_skip_silence),
    Smile(R.drawable.ic_steno_smile),
    Speaker(R.drawable.ic_steno_speaker),
    Speed(R.drawable.ic_steno_speed),
    Stop(R.drawable.ic_steno_stop),
    Tasks(R.drawable.ic_steno_tasks),
    Warning(R.drawable.ic_steno_warning),
    Wave(R.drawable.ic_steno_wave),
    ;

    val painter: Painter
        @Composable get() = painterResource(resId)
}

/** Иллюстрации онбординга, пустых состояний и ошибок (240×180, свои цвета, одинаковы в обеих темах). */
enum class StenoIllustrations(@param:DrawableRes val resId: Int) {
    Listen(R.drawable.ill_steno_onb_1_listen),
    Private(R.drawable.ill_steno_onb_2_private),
    Models(R.drawable.ill_steno_onb_3_models),
    Microphone(R.drawable.ill_steno_onb_4_mic),
    EmptyHistory(R.drawable.ill_steno_empty_history),
    ErrorFormat(R.drawable.ill_steno_error_format),
    ErrorMic(R.drawable.ill_steno_error_mic),
    ErrorModel(R.drawable.ill_steno_error_model),
    ;

    val painter: Painter
        @Composable get() = painterResource(resId)
}
