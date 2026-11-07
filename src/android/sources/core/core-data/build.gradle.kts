plugins {
    alias(libs.plugins.steno.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.metro)
}

android {
    namespace = "ru.nuclearhack.stenographer.core.data"
}

dependencies {
    implementation(projects.sources.core.coreDomain)
    implementation(projects.sources.common.commonAudio)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
}
