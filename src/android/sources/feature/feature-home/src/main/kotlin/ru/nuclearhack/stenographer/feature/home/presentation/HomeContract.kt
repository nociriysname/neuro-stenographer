package ru.nuclearhack.stenographer.feature.home.presentation

import ru.nuclearhack.stenographer.core.domain.engine.EngineInfo
import ru.nuclearhack.stenographer.core.model.AudioRef

data class HomeState(
    val engines: List<EngineInfo> = emptyList(),
)

sealed interface HomeAction {
    /** [uri] == null — пользователь закрыл системный выбор файла. */
    data class AudioPicked(val uri: String?) : HomeAction
}

sealed interface HomeEffect {
    data class OpenTranscript(val audio: AudioRef) : HomeEffect
}
