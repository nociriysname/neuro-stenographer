plugins {
    alias(libs.plugins.steno.android.application)
    alias(libs.plugins.steno.android.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

android {
    namespace = "ru.nuclearhack.stenographer"

    defaultConfig {
        applicationId = "ru.nuclearhack.stenographer"
        versionCode = 1
        versionName = "0.1.0"
        ndk { abiFilters += "arm64-v8a" }
    }

    // Веса моделей лежат в assets без сжатия: копируются на устройство быстро и без распаковки в память.
    androidResources {
        noCompress += listOf("gguf", "onnx")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Для сдачи на хакатоне release подписывается debug-ключом, чтобы APK ставился без лишних шагов.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    // Composition root видит все слои: здесь Metro собирает граф из @Contributes* всех модулей.
    implementation(projects.sources.common.commonUikit)
    implementation(projects.sources.common.commonAudio)
    implementation(projects.sources.common.commonModels)
    implementation(projects.sources.core.coreModel)
    implementation(projects.sources.core.coreDomain)
    implementation(projects.sources.core.coreData)
    implementation(projects.sources.core.coreAsr)
    implementation(projects.sources.core.coreDiarization)
    implementation(projects.sources.core.coreLlm)
    implementation(projects.sources.feature.featureHome)
    implementation(projects.sources.feature.featureTranscript)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.metrox.android)
    implementation(libs.metrox.viewmodel.compose)
}
