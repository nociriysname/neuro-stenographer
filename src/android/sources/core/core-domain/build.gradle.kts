plugins {
    alias(libs.plugins.steno.jvm.library)
    alias(libs.plugins.metro)
}

dependencies {
    api(projects.sources.core.coreModel)
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
}
