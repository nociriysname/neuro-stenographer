plugins {
    alias(libs.plugins.steno.android.feature)
}

android {
    namespace = "ru.nuclearhack.stenographer.feature.settings"
}

dependencies {
    // PcmAudio из сигнатуры SpeechRecognizer нужен тестовому fake-распознавателю.
    testImplementation(project(":sources:core:core-model"))
}
