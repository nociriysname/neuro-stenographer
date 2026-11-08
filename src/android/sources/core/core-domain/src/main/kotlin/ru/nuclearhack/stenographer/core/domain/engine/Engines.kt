package ru.nuclearhack.stenographer.core.domain.engine

import ru.nuclearhack.stenographer.core.model.MeetingProtocol
import ru.nuclearhack.stenographer.core.model.PcmAudio
import ru.nuclearhack.stenographer.core.model.Transcript

/*
 * Порты ML-движков. Domain описывает, ЧТО нужно конвейеру; реализации (sherpa-onnx, NeMo-Speech.cpp,
 * llama.cpp) живут в модулях core-asr / core-diarization / core-llm и подключаются через Metro.
 */

enum class EngineKind { Diarization, SpeechRecognition, Llm }

/** Что за модель и готова ли она к работе — для экрана статуса, отчёта и защиты. */
data class EngineInfo(
    val kind: EngineKind,
    val modelName: String,
    val runtime: String,
    val isReady: Boolean,
)

/** Движок не может работать: модель не подключена, не найдена на устройстве или не загрузилась. */
class EngineUnavailableException(
    val kind: EngineKind,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/** Слово с абсолютными таймкодами (мс от начала записи). Пунктуация, если есть, приклеена к слову. */
data class RecognizedWord(
    val text: String,
    val startMs: Long,
    val endMs: Long,
)

interface SpeechRecognizer {
    val info: EngineInfo

    /**
     * Распознаёт короткий фрагмент записи. GigaAM обучалась на отрезках до ~25 с, поэтому длинные записи
     * сначала режет [ru.nuclearhack.stenographer.core.domain.processing.ChunkPlanner].
     * Таймкоды слов возвращаются уже со сдвигом [offsetMs].
     */
    suspend fun recognize(audio: PcmAudio, offsetMs: Long): List<RecognizedWord>

    /**
     * Держит модель загруженной, пока выполняется [block]: конвейер распознаёт фрагменты один за другим,
     * и загружать сотни мегабайт весов на каждый было бы слишком долго. Вне [block] [recognize] загружает
     * модель сам на один вызов. По умолчанию просто выполняет [block] — для движков без дорогой загрузки.
     */
    suspend fun <T> withModelLoaded(block: suspend () -> T): T = block()

    /** Есть ли на устройстве модель для режима с пунктуацией или без — для настроек. */
    fun supportsPunctuationMode(punctuated: Boolean): Boolean = true
}

/** Интервал речи одного говорящего. [speaker] — канал модели в порядке первого появления (с нуля). */
data class SpeakerSegment(
    val speaker: Int,
    val startMs: Long,
    val endMs: Long,
)

interface Diarizer {
    val info: EngineInfo

    /** Возвращает сегменты речи, отсортированные по началу. Сегменты разных спикеров могут перекрываться. */
    suspend fun diarize(audio: PcmAudio, onProgress: (Float) -> Unit = {}): List<SpeakerSegment>
}

/**
 * Постобработка расшифровки локальной LLM.
 *
 * Контракт: менять можно только пунктуацию, регистр и метаданные. Слова менять нельзя, иначе растёт WER,
 * а он даёт 40 баллов из 100. Реализация обязана проверять это и откатывать правки, которые меняют слова.
 */
interface TranscriptPostProcessor {
    val info: EngineInfo

    suspend fun process(transcript: Transcript): Transcript
}

/**
 * Протокол встречи от локальной LLM: краткое содержание, решения, задачи с исполнителями и сроками.
 *
 * Работает поверх готовой расшифровки и не меняет её текст. Длинные записи реализация режет на части
 * и объединяет протоколы частей. [onProgress] получает долю от 0 до 1.
 */
interface ProtocolGenerator {
    val info: EngineInfo

    suspend fun generate(transcript: Transcript, onProgress: (Float) -> Unit = {}): MeetingProtocol
}
