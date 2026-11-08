package ru.nuclearhack.stenographer.core.llm

/** Колбэки генерации из нативного кода. false — отмена. */
internal interface GenerationListener {
    fun onPrefill(done: Int, total: Int): Boolean

    fun onToken(generated: Int): Boolean
}

/**
 * Тонкая обёртка над JNI (src/main/cpp/llm_jni.cpp). Хэндл — нативный указатель на модель и контекст,
 * вызывающий код обязан освобождать его в finally.
 */
internal object NativeLlm {

    init {
        System.loadLibrary("steno_llm")
    }

    @JvmStatic external fun nativeLoad(modelPath: String, threads: Int, contextTokens: Int): Long

    @JvmStatic external fun nativeFree(handle: Long)

    @JvmStatic external fun nativeCountTokens(handle: Long, text: String): Int

    @JvmStatic external fun nativeGenerate(
        handle: Long,
        prompt: String,
        maxTokens: Int,
        listener: GenerationListener,
    ): String
}
