plugins {
    alias(libs.plugins.steno.android.library)
    alias(libs.plugins.steno.android.compose)
}

android {
    namespace = "ru.nuclearhack.stenographer.common.uikit"
}

dependencies {
    api(libs.androidx.lifecycle.viewmodel)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(libs.kotlinx.coroutines.test)
}
