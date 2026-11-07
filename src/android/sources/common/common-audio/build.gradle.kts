plugins {
    alias(libs.plugins.steno.android.library)
    alias(libs.plugins.metro)
}

android {
    namespace = "ru.nuclearhack.stenographer.common.audio"
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
}
