package ru.nuclearhack.stenographer.core.diarization

/**
 * Тонкая обёртка над JNI (src/main/cpp/diar_jni.cpp). Библиотека грузится при первом обращении,
 * а не при старте приложения. Ошибки C API приходят как [IllegalStateException].
 *
 * Хэндлы — нативные указатели; вызывающий код обязан освобождать их в finally.
 */
internal object NativeDiarizer {

    init {
        System.loadLibrary("steno_diar")
    }

    @JvmStatic external fun nativeCreate(modelPath: String, preset: String): Long

    @JvmStatic external fun nativeDestroy(model: Long)

    @JvmStatic external fun nativeStreamOpen(model: Long): Long

    @JvmStatic external fun nativeStreamPush(
        stream: Long,
        samples: FloatArray,
        offset: Int,
        count: Int,
        sampleRate: Int,
    )

    @JvmStatic external fun nativeStreamFinish(stream: Long)

    /** Плоский массив `[startSec, endSec, speaker(с 1)] × N`. Нулевые параметры — значения библиотеки. */
    @JvmStatic external fun nativeStreamSegments(
        stream: Long,
        onset: Float,
        offset: Float,
        padOnsetSec: Double,
        padOffsetSec: Double,
        minGapSec: Double,
        minDurationSec: Double,
    ): DoubleArray

    @JvmStatic external fun nativeStreamClose(stream: Long)
}
