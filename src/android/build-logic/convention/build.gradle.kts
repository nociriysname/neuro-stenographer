plugins {
    `kotlin-dsl`
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    // compileOnly: в рантайме AGP берётся из classpath корневого проекта (plugins { ... apply false }).
    compileOnly(libs.android.gradlePlugin)
    // Классы задач detekt для конвенции steno.detekt (сам DetektPlugin не применяем).
    implementation(libs.detekt.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "steno.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "steno.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "steno.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "steno.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("jvmLibrary") {
            id = "steno.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}
