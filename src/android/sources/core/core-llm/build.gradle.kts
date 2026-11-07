plugins {
    alias(libs.plugins.steno.android.library)
    alias(libs.plugins.metro)
}

android {
    namespace = "ru.nuclearhack.stenographer.core.llm"

    // Следующий шаг: llama.cpp через CMake. Его ggml общий с NeMo-Speech.cpp (llama.cpp там сабмодуль),
    // поэтому логично собрать диаризацию и LLM в одну нативную библиотеку.
}

dependencies {
    implementation(projects.sources.core.coreDomain)
}
