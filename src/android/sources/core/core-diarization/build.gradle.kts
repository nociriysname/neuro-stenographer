plugins {
    alias(libs.plugins.steno.android.library)
    alias(libs.plugins.metro)
}

android {
    namespace = "ru.nuclearhack.stenographer.core.diarization"

    defaultConfig {
        // Только arm64: современные телефоны и эмулятор на Apple Silicon. Вдвое меньше APK и время сборки.
        ndk { abiFilters += "arm64-v8a" }
        externalNativeBuild {
            cmake {
                arguments += "-DSTENO_THIRD_PARTY_DIR=$rootDir/third_party"
                targets += "steno_diar"
            }
        }
    }

    // Модель лежит в assets модуля и попадает в APK инструментальных тестов: без сжатия она копируется быстрее.
    androidResources {
        noCompress += "gguf"
    }

    // NeMo-Speech.cpp (диаризация) + ggml + SentencePiece → libsteno_diar.so, см. src/main/cpp/CMakeLists.txt.
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
}

dependencies {
    implementation(projects.sources.core.coreDomain)
    implementation(projects.sources.common.commonModels)
    implementation(libs.kotlinx.coroutines.android)
}
