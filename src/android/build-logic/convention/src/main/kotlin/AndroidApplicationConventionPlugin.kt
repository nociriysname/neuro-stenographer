import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")

            extensions.configure<ApplicationExtension> {
                compileSdk = libs.versionInt("android-compileSdk")
                // Один NDK на все модули: им собираются нативные движки, им же AGP вырезает отладочные символы из .so в APK.
                ndkVersion = libs.findVersion("android-ndk").get().requiredVersion
                defaultConfig.minSdk = libs.versionInt("android-minSdk")
                defaultConfig.targetSdk = libs.versionInt("android-targetSdk")
                compileOptions {
                    sourceCompatibility = STENO_JAVA_VERSION
                    targetCompatibility = STENO_JAVA_VERSION
                }
            }
        }
    }
}
