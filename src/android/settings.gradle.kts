pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // sherpa-onnx публикует AAR только в GitHub Releases. tools/fetch_models.sh кладёт его сюда
        // в раскладке Maven (с проверкой SHA-256), а Gradle берёт как обычную внешнюю зависимость.
        maven {
            name = "thirdPartyPrebuilt"
            url = uri("third_party/maven")
            metadataSources { artifact() }
            content { includeGroup("com.k2fsa.sherpa.onnx") }
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "neuro-stenographer"

// Composition root: Application, граф Metro, навигация. Единственный модуль, который видит все слои.
include(":app")

// common — платформенная инфраструктура без знания предметной области (UI-kit, звук, доставка весов моделей).
include(":sources:common:common-uikit")
include(":sources:common:common-audio")
include(":sources:common:common-models")

// core — предметная область. Слои разведены модулями:
//   core-model, core-domain — чистый Kotlin/JVM, без Android;
//   core-data, core-asr, core-diarization, core-llm — реализации портов domain (data-слой).
include(":sources:core:core-model")
include(":sources:core:core-domain")
include(":sources:core:core-data")
include(":sources:core:core-asr")
include(":sources:core:core-diarization")
include(":sources:core:core-llm")

// feature — только presentation. Зависят от core-domain и common-uikit, но не от data/ML-модулей.
include(":sources:feature:feature-home")
include(":sources:feature:feature-transcript")
include(":sources:feature:feature-onboarding")
include(":sources:feature:feature-settings")

// Свои правила detekt (подключаются конвенцией steno.detekt ко всем модулям).
include(":detekt-rules")
