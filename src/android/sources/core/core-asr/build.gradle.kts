plugins {
    alias(libs.plugins.steno.android.library)
    alias(libs.plugins.metro)
}

android {
    namespace = "ru.nuclearhack.stenographer.core.asr"
}

dependencies {
    implementation(projects.sources.core.coreDomain)

    // Следующий шаг: AAR sherpa-onnx (GitHub Releases k2-fsa/sherpa-onnx, v2.0.1, не Maven Central)
    // + веса sherpa-onnx-nemo-transducer-punct-giga-am-v3-russian-2025-12-16 в assets.
}
