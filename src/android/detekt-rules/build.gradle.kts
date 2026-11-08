import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.steno.jvm.library)
}

// detekt-cli 1.23.8 работает на Kotlin 2.0.21: правила собираются под API stdlib 2.0, иначе в рантайме
// возможен NoSuchMethodError на функциях новее. В CuApp стоит 1.9, но Kotlin 2.4.20 его уже не принимает.
kotlin {
    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
    }
}

dependencies {
    compileOnly(libs.detekt.api)
    testImplementation(libs.detekt.api)
    testImplementation(libs.detekt.test)
}
