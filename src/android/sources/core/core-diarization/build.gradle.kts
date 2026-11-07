plugins {
    alias(libs.plugins.steno.android.library)
    alias(libs.plugins.metro)
}

android {
    namespace = "ru.nuclearhack.stenographer.core.diarization"

    // Следующий шаг: externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt") } }
    // с NeMo-Speech.cpp (диаризация, CPU-бэкенд ggml) и JNI-обёрткой над nemo_speech/diar.h.
    // Нужен NDK: sdkmanager "ndk;<версия>" — сейчас на машине не установлен.
}

dependencies {
    implementation(projects.sources.core.coreDomain)
}
