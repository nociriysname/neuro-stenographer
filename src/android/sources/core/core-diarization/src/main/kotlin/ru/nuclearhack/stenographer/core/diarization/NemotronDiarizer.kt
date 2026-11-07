package ru.nuclearhack.stenographer.core.diarization

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.File
import kotlin.math.roundToLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ru.nuclearhack.stenographer.common.models.ModelAsset
import ru.nuclearhack.stenographer.common.models.ModelInstaller
import ru.nuclearhack.stenographer.common.models.ModelNotFoundException
import ru.nuclearhack.stenographer.core.domain.engine.Diarizer
import ru.nuclearhack.stenographer.core.domain.engine.EngineInfo
import ru.nuclearhack.stenographer.core.domain.engine.EngineKind
import ru.nuclearhack.stenographer.core.domain.engine.EngineUnavailableException
import ru.nuclearhack.stenographer.core.domain.engine.SpeakerSegment
import ru.nuclearhack.stenographer.core.model.PcmAudio

/**
 * Диаризация nvidia/Nemotron-3-Diarization (Sortformer + AOSC, до 8 говорящих, OpenMDW-1.1)
 * через NeMo-Speech.cpp (ggml, CPU) и JNI.
 *
 * Модель загружается на время одного вызова и сразу освобождается: конвейер грузит модели по очереди,
 * чтобы на телефоне в памяти одновременно была только одна. GGUF открывается через mmap, загрузка быстрая.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class NemotronDiarizer(
    private val modelInstaller: ModelInstaller,
) : Diarizer {

    override val info: EngineInfo
        get() = EngineInfo(
            kind = EngineKind.Diarization,
            modelName = "Nemotron-3-Diarization q8_0",
            runtime = "NeMo-Speech.cpp (ggml)",
            isReady = modelInstaller.isAvailable(MODEL),
        )

    /** Параметры постобработки сегментов; null — значения библиотеки (подобраны NVIDIA на CALLHOME). */
    data class Segmentation(
        val onset: Float? = null,
        val offset: Float? = null,
        val padOnsetSec: Double? = null,
        val padOffsetSec: Double? = null,
        val minGapSec: Double? = null,
        val minDurationSec: Double? = null,
    )

    var segmentation: Segmentation = Segmentation()

    private val mutex = Mutex()

    override suspend fun diarize(audio: PcmAudio, onProgress: (Float) -> Unit): List<SpeakerSegment> {
        require(audio.sampleRate == PcmAudio.SAMPLE_RATE_16K) {
            "Nemotron ждёт 16 кГц, пришло ${audio.sampleRate} Гц"
        }
        val modelFile = try {
            modelInstaller.install(MODEL)
        } catch (e: ModelNotFoundException) {
            throw EngineUnavailableException(EngineKind.Diarization, e.message ?: "Нет файла модели", e)
        }
        return mutex.withLock {
            withContext(Dispatchers.Default) { run(modelFile, audio, onProgress) }
        }
    }

    private suspend fun run(modelFile: File, audio: PcmAudio, onProgress: (Float) -> Unit): List<SpeakerSegment> {
        val model = createModel(modelFile)
        try {
            val stream = NativeDiarizer.nativeStreamOpen(model)
            try {
                val samples = audio.samples
                var offset = 0
                while (offset < samples.size) {
                    currentCoroutineContext().ensureActive()
                    val count = minOf(PUSH_CHUNK_SAMPLES, samples.size - offset)
                    NativeDiarizer.nativeStreamPush(stream, samples, offset, count, audio.sampleRate)
                    offset += count
                    // Хвост записи считается в finish(), поэтому до него показываем не больше 90 %.
                    onProgress(0.9f * offset / samples.size)
                }
                NativeDiarizer.nativeStreamFinish(stream)
                onProgress(1f)

                val config = segmentation
                return parseSegments(
                    NativeDiarizer.nativeStreamSegments(
                        stream,
                        onset = config.onset ?: 0f,
                        offset = config.offset ?: 0f,
                        padOnsetSec = config.padOnsetSec ?: 0.0,
                        padOffsetSec = config.padOffsetSec ?: 0.0,
                        minGapSec = config.minGapSec ?: 0.0,
                        minDurationSec = config.minDurationSec ?: 0.0,
                    ),
                )
            } finally {
                NativeDiarizer.nativeStreamClose(stream)
            }
        } finally {
            NativeDiarizer.nativeDestroy(model)
        }
    }

    private fun createModel(modelFile: File): Long = try {
        NativeDiarizer.nativeCreate(modelFile.absolutePath, PRESET)
    } catch (e: LinkageError) {
        throw EngineUnavailableException(EngineKind.Diarization, "Не загрузилась нативная библиотека диаризации", e)
    } catch (e: IllegalStateException) {
        throw EngineUnavailableException(EngineKind.Diarization, "Не удалось загрузить Nemotron: ${e.message}", e)
    }

    companion object {
        val MODEL = ModelAsset(fileName = "Nemotron-3-Diarization.q8_0.gguf", sizeBytes = 107_012_128)

        /** Офлайн-геометрия V3: буфер 30.4 с, кэш спикеров 264 кадра — лучший DER для готовых записей. */
        const val PRESET = "v3-offline"

        /** Порция для push: 2 с. Модель всё равно считает целыми чанками, это шаг прогресса и отмены. */
        private const val PUSH_CHUNK_SAMPLES = 2 * PcmAudio.SAMPLE_RATE_16K
    }
}

/** `[startSec, endSec, speaker(с 1)] × N` → доменные сегменты (спикер с нуля, время в мс), по началу. */
internal fun parseSegments(raw: DoubleArray): List<SpeakerSegment> {
    require(raw.size % 3 == 0) { "Ожидались тройки значений, пришло ${raw.size}" }
    return (raw.indices step 3)
        .map { i ->
            SpeakerSegment(
                speaker = raw[i + 2].toInt() - 1,
                startMs = (raw[i] * 1000).roundToLong(),
                endMs = (raw[i + 1] * 1000).roundToLong(),
            )
        }
        .filter { it.endMs > it.startMs && it.speaker >= 0 }
        .sortedBy { it.startMs }
}
