import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Feature-модуль = только presentation-слой.
 *
 * Сюда намеренно подключены лишь core-domain и common-uikit: фича вызывает use case'ы и не знает,
 * чем они реализованы. Модули core-data / core-asr / core-diarization / core-llm видит только :app.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("steno.android.library")
            pluginManager.apply("steno.android.compose")
            pluginManager.apply("dev.zacsweers.metro")

            dependencies {
                add("implementation", project(":sources:common:common-uikit"))
                add("implementation", project(":sources:core:core-domain"))

                add("implementation", libs.library("metrox-viewmodel-compose"))
                add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
                add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.library("androidx-activity-compose"))

                add("testImplementation", libs.library("kotlinx-coroutines-test"))
            }
        }
    }
}
