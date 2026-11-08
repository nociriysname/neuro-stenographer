plugins {
    alias(libs.plugins.steno.android.library)
    alias(libs.plugins.metro)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "ru.nuclearhack.stenographer.core.llm"

    defaultConfig {
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
        externalNativeBuild {
            cmake {
                arguments += "-DSTENO_THIRD_PARTY_DIR=$rootDir/third_party"
                targets += "steno_llm"
            }
        }
    }

    androidResources {
        noCompress += "gguf"
    }

    // llama.cpp (свежий, с LFM3) → libsteno_llm.so, см. src/main/cpp/CMakeLists.txt.
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
    implementation(libs.kotlinx.serialization.json)
}
