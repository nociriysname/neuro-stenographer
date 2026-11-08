import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Чистый Kotlin/JVM-модуль: для слоёв, которым запрещено знать об Android (model, domain). */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            pluginManager.apply("steno.detekt")

            extensions.configure<JavaPluginExtension> {
                toolchain.languageVersion.set(JavaLanguageVersion.of(STENO_JAVA_TOOLCHAIN))
            }

            dependencies {
                add("testImplementation", libs.library("junit"))
                add("testImplementation", libs.library("kotlin-test-junit"))
            }
        }
    }
}
