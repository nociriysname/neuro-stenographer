package ru.nuclearhack.stenographer.core.asr

import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ru.nuclearhack.stenographer.common.models.ModelInstaller
import ru.nuclearhack.stenographer.common.models.ModelNotFoundException
import ru.nuclearhack.stenographer.core.domain.engine.EngineInfo
import ru.nuclearhack.stenographer.core.domain.engine.EngineKind
import ru.nuclearhack.stenographer.core.domain.engine.EngineUnavailableException
import ru.nuclearhack.stenographer.core.domain.engine.RecognizedWord
import ru.nuclearhack.stenographer.core.domain.engine.SpeechRecognizer
import ru.nuclearhack.stenographer.core.domain.settings.SettingsRepository
import ru.nuclearhack.stenographer.core.model.PcmAudio
import java.io.File

/**
 * Распознавание речи GigaAM-v3 (ai-sage/GigaAM-v3, MIT) через sherpa-onnx: RNN-T, энкодер int8, CPU.
 *
 * Модель весит ~230 МБ, поэтому конвейер держит её загруженной на весь этап ASR через [withModelLoaded]
 * и освобождает до LLM-этапа. Фрагменты декодируются по одному: длинные записи заранее режет ChunkPlanner.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class GigaAmSpeechRecognizer(
    private val modelInstaller: ModelInstaller,
    private val settingsRepository: SettingsRepository,
) : SpeechRecognizer {

    /** Вариант для замеров. null — по настройке «С пунктуацией» / «Без пунктуации». */
    var preferredVariant: GigaAmVariant? = null

    /** Выбранный вариант, если его веса есть на устройстве, иначе любой доступный (в APK кладётся один). */
    val variant: GigaAmVariant?
        get() = (preferredVariant ?: variantFromSettings()).takeIf { modelInstaller.isAvailable(it.assets) }
            ?: GigaAmVariant.entries.firstOrNull { modelInstaller.isAvailable(it.assets) }

    private fun variantFromSettings(): GigaAmVariant {
        val punctuation = settingsRepository.settings.value.punctuation
        return GigaAmVariant.entries.first { it.punctuated == punctuation }
    }

    override fun supportsPunctuationMode(punctuated: Boolean): Boolean =
        GigaAmVariant.entries.any { it.punctuated == punctuated && modelInstaller.isAvailable(it.assets) }

    override val info: EngineInfo
        get() {
            val available = variant
            return EngineInfo(
                kind = EngineKind.SpeechRecognition,
                modelName = "GigaAM-v3 ${(available ?: GigaAmVariant.E2eRnnt).title}",
                runtime = "sherpa-onnx (ONNX Runtime, CPU)",
                isReady = available != null,
            )
        }

    /** Потоки ONNX Runtime. Больше четырёх на телефоне не помогает: остальные ядра — энергоэффективные. */
    var numThreads: Int = Runtime.getRuntime().availableProcessors().coerceIn(1, MAX_THREADS)

    private val decodeMutex = Mutex()

    @Volatile
    private var session: OfflineRecognizer? = null

    override suspend fun <T> withModelLoaded(block: suspend () -> T): T {
        if (session != null) return block()
        val recognizer = load()
        session = recognizer
        try {
            return block()
        } finally {
            session = null
            decodeMutex.withLock { recognizer.release() }
        }
    }

    override suspend fun recognize(audio: PcmAudio, offsetMs: Long): List<RecognizedWord> {
        require(audio.sampleRate == PcmAudio.SAMPLE_RATE_16K) {
            "GigaAM ждёт 16 кГц, пришло ${audio.sampleRate} Гц"
        }
        // Меньше пары кадров энкодер не обработает, а слов в таком обрывке всё равно нет.
        if (audio.durationMs < MIN_AUDIO_MS) return emptyList()

        session?.let { return decode(it, audio, offsetMs) }
        val recognizer = load()
        try {
            return decode(recognizer, audio, offsetMs)
        } finally {
            recognizer.release()
        }
    }

    private suspend fun load(): OfflineRecognizer {
        val files = install(variant ?: throw unavailable("Нет весов GigaAM-v3: их кладёт в APK tools/fetch_models.sh"))
        return withContext(Dispatchers.Default) {
            try {
                OfflineRecognizer(config = recognizerConfig(files))
            } catch (e: LinkageError) {
                throw unavailable("Не загрузилась нативная библиотека sherpa-onnx", e)
            }
        }
    }

    private class ModelFiles(val encoder: File, val decoder: File, val joiner: File, val tokens: File)

    private suspend fun install(variant: GigaAmVariant): ModelFiles = try {
        ModelFiles(
            encoder = modelInstaller.install(variant.encoder),
            decoder = modelInstaller.install(variant.decoder),
            joiner = modelInstaller.install(variant.joiner),
            tokens = modelInstaller.install(variant.tokens),
        )
    } catch (e: ModelNotFoundException) {
        throw unavailable(e.message ?: "Нет файла модели", e)
    }

    private fun unavailable(message: String, cause: Throwable? = null) =
        EngineUnavailableException(EngineKind.SpeechRecognition, message, cause)

    private fun recognizerConfig(files: ModelFiles) = OfflineRecognizerConfig(
        // GigaAM: 64 mel-полосы. Остальное (окно, частоты) sherpa-onnx берёт по флагу is_giga_am из модели.
        featConfig = FeatureConfig(sampleRate = PcmAudio.SAMPLE_RATE_16K, featureDim = MEL_BANDS),
        modelConfig = OfflineModelConfig(
            transducer = OfflineTransducerModelConfig(
                encoder = files.encoder.absolutePath,
                decoder = files.decoder.absolutePath,
                joiner = files.joiner.absolutePath,
            ),
            tokens = files.tokens.absolutePath,
            modelType = "nemo_transducer",
            numThreads = numThreads,
            provider = "cpu",
        ),
        decodingMethod = "greedy_search",
    )

    private suspend fun decode(recognizer: OfflineRecognizer, audio: PcmAudio, offsetMs: Long): List<RecognizedWord> =
        decodeMutex.withLock {
            withContext(Dispatchers.Default) {
                val stream = recognizer.createStream()
                try {
                    stream.acceptWaveform(audio.samples, audio.sampleRate)
                    recognizer.decode(stream)
                    val result = recognizer.getResult(stream)
                    tokensToWords(result.tokens, result.timestamps, offsetMs, audio.durationMs)
                } finally {
                    stream.release()
                }
            }
        }

    private companion object {
        const val MIN_AUDIO_MS = 100L
        const val MEL_BANDS = 64
        const val MAX_THREADS = 4
    }
}
