plugins {
    alias(libs.plugins.steno.android.library)
    alias(libs.plugins.metro)
}

android {
    namespace = "ru.nuclearhack.stenographer.core.asr"

    defaultConfig {
        // В AAR sherpa-onnx библиотеки под 4 ABI; берём arm64 (телефоны) и x86_64 (стандартный эмулятор).
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
    }

    // Веса лежат в assets модуля и попадают в APK инструментальных тестов: без сжатия копируются быстрее.
    androidResources {
        noCompress += "onnx"
    }
}

dependencies {
    implementation(projects.sources.core.coreDomain)
    implementation(projects.sources.common.commonModels)
    implementation(libs.kotlinx.coroutines.android)

    // sherpa-onnx с ONNX Runtime внутри. AAR из GitHub Releases кладёт tools/fetch_models.sh; POM у него нет,
    // поэтому тип артефакта указываем явно.
    implementation(variantOf(libs.sherpa.onnx) { artifactType("aar") })

    // Инструментальный тест декодирует запись тем же декодером, что и приложение.
    androidTestImplementation(projects.sources.common.commonAudio)
}
