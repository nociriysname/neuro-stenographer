import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Android-библиотека: SDK-уровни, версия Java и тестовые зависимости. Kotlin встроен в AGP 9. */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")

            extensions.configure<LibraryExtension> {
                compileSdk = libs.versionInt("android-compileSdk")
                defaultConfig.minSdk = libs.versionInt("android-minSdk")
                compileOptions {
                    sourceCompatibility = STENO_JAVA_VERSION
                    targetCompatibility = STENO_JAVA_VERSION
                }
            }

            dependencies {
                add("testImplementation", libs.library("junit"))
                add("testImplementation", libs.library("kotlin-test-junit"))
            }
        }
    }
}
