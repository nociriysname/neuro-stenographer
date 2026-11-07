package ru.nuclearhack.stenographer.core.asr

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import ru.nuclearhack.stenographer.core.domain.engine.EngineInfo
import ru.nuclearhack.stenographer.core.domain.engine.EngineKind
import ru.nuclearhack.stenographer.core.domain.engine.EngineUnavailableException
import ru.nuclearhack.stenographer.core.domain.engine.RecognizedWord
import ru.nuclearhack.stenographer.core.domain.engine.SpeechRecognizer
import ru.nuclearhack.stenographer.core.model.PcmAudio

/**
 * Распознавание речи GigaAM-v3 (ai-sage/GigaAM-v3, MIT) через sherpa-onnx / ONNX Runtime.
 *
 * Пока не подключено: нет AAR sherpa-onnx и весов. План — docs/ARCHITECTURE.md, раздел «GigaAM-v3».
 * Реализация: OfflineRecognizer (model_type nemo_transducer), токены + таймкоды → слова со сдвигом offsetMs.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class GigaAmSpeechRecognizer : SpeechRecognizer {

    override val info = EngineInfo(
        kind = EngineKind.SpeechRecognition,
        modelName = "GigaAM-v3 e2e_rnnt",
        runtime = "sherpa-onnx",
        isReady = false,
    )

    override suspend fun recognize(audio: PcmAudio, offsetMs: Long): List<RecognizedWord> =
        throw EngineUnavailableException(info.kind, "GigaAM-v3 ещё не подключена к приложению")
}
