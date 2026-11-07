package ru.nuclearhack.stenographer.core.diarization

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import ru.nuclearhack.stenographer.core.domain.engine.Diarizer
import ru.nuclearhack.stenographer.core.domain.engine.EngineInfo
import ru.nuclearhack.stenographer.core.domain.engine.EngineKind
import ru.nuclearhack.stenographer.core.domain.engine.EngineUnavailableException
import ru.nuclearhack.stenographer.core.domain.engine.SpeakerSegment
import ru.nuclearhack.stenographer.core.model.PcmAudio

/**
 * Диаризация nvidia/Nemotron-3-Diarization (Sortformer + AOSC, до 8 говорящих, OpenMDW-1.1)
 * через NeMo-Speech.cpp (C++/ggml, C ABI `nemo_speech/diar.h`) и JNI.
 *
 * Пока не подключено: нужна NDK-сборка. План — docs/ARCHITECTURE.md, раздел «Nemotron-3-Diarization».
 * Реализация: nemo_speech_diar_create(preset = "v3-offline") → stream push/finish → segments(cfg).
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class NemotronDiarizer : Diarizer {

    override val info = EngineInfo(
        kind = EngineKind.Diarization,
        modelName = "Nemotron-3-Diarization q8_0",
        runtime = "NeMo-Speech.cpp (ggml)",
        isReady = false,
    )

    override suspend fun diarize(audio: PcmAudio, onProgress: (Float) -> Unit): List<SpeakerSegment> =
        throw EngineUnavailableException(info.kind, "Nemotron-3-Diarization ещё не подключена к приложению")
}
