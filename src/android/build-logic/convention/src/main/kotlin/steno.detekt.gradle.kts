import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.DetektGenerateConfigTask
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.reporting.ReportingExtension
import org.gradle.jvm.toolchain.JavaToolchainService

/*
 * detekt по образцу CuApp (cu.detekt + cu.quality). Подключается из всех convention-плагинов модулей.
 *
 * DetektPlugin.apply() не используем: он зовёт ReportingExtension.file(), которого нет в Gradle 10,
 * и регистрирует ненужные задачи по вариантам Android. Берём только классы задач и движок detekt-cli.
 *
 *   ./gradlew detekt            — проверка всех модулей (входит в check)
 *   ./gradlew detekt -PdetektAutoCorrect — заодно исправить форматирование (detekt-formatting)
 *   ./gradlew detektBaseline    — зафиксировать текущие замечания модуля в baseline
 */
plugins { `reporting-base` }

val settings = extensions.create<DetektExtension>("detekt")
settings.reportsDir = the<ReportingExtension>().baseDirectory.dir("detekt").get().asFile
settings.config.setFrom(rootProject.layout.projectDirectory.file("config/detekt/detekt.yml"))
settings.buildUponDefaultConfig = true
settings.parallel = false
settings.autoCorrect = providers.gradleProperty("detektAutoCorrect").map { it != "false" }.getOrElse(false)
settings.source.setFrom(files("src/main/kotlin", "src/test/kotlin", "src/androidTest/kotlin"))
val moduleBaseline = layout.projectDirectory.file("detekt-baseline.xml").asFile
settings.baseline = moduleBaseline

val engine = configurations.create("detekt") {
    isCanBeConsumed = false
    isCanBeResolved = true
    defaultDependencies {
        add(project.dependencies.create("io.gitlab.arturbosch.detekt:detekt-cli:${settings.toolVersion}"))
    }
}
val rules = configurations.create("detektPlugins") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    add("detektPlugins", libs.library("detekt-formatting"))
    // Свои правила (src/android/detekt-rules). Без транзитивных зависимостей: Kotlin stdlib даёт detekt-cli.
    if (path != ":detekt-rules") {
        add("detektPlugins", project(":detekt-rules")) { isTransitive = false }
    }
}

val configuredSource = providers.provider { settings.source.filter { it.exists() } }
val configuredBaseline = layout.file(providers.provider { settings.baseline?.takeIf { it.isFile } })

tasks.withType<Detekt>().configureEach {
    detektClasspath.setFrom(engine)
    pluginClasspath.setFrom(rules)
    config.setFrom(settings.config)
    debug = settings.debug
    parallel = settings.parallel
    disableDefaultRuleSets = settings.disableDefaultRuleSets
    buildUponDefaultConfig = settings.buildUponDefaultConfig
    allRules = settings.allRules
    autoCorrect = settings.autoCorrect
    ignoreFailures = settings.ignoreFailures
    settings.basePath?.let { basePath = it }
    reports {
        html.required.set(true)
        xml.required.set(false)
        txt.required.set(false)
        sarif.required.set(false)
        md.required.set(false)
    }
}
val analysis = tasks.register<Detekt>("detekt") {
    description = "Checks production and test sources with Detekt."
    setSource(configuredSource)
    include("**/*.kt", "**/*.kts")
    exclude("**/build/**", "**/generated/**", "**/resources/**")
    baseline.convention(configuredBaseline)
    reportsDir.convention(providers.provider { settings.reportsDir })
}
tasks.matching { it.name == "check" }.configureEach { dependsOn(analysis) }

tasks.register<DetektCreateBaselineTask>("detektBaseline") {
    description = "Records current Detekt findings of the module in detekt-baseline.xml."
    detektClasspath.setFrom(engine)
    pluginClasspath.setFrom(rules)
    setSource(configuredSource)
    include("**/*.kt", "**/*.kts")
    exclude("**/build/**", "**/generated/**", "**/resources/**")
    config.setFrom(settings.config)
    baseline.convention(layout.file(providers.provider { settings.baseline }))
    debug.convention(providers.provider { settings.debug })
    parallel.convention(providers.provider { settings.parallel })
    disableDefaultRuleSets.convention(providers.provider { settings.disableDefaultRuleSets })
    buildUponDefaultConfig.convention(providers.provider { settings.buildUponDefaultConfig })
    allRules.convention(providers.provider { settings.allRules })
    autoCorrect.convention(providers.provider { settings.autoCorrect })
    ignoreFailures.convention(providers.provider { settings.ignoreFailures })
    settings.basePath?.let { basePath = it }
}

// JVM-модули проверяются тем же JDK, что и компилируются.
pluginManager.withPlugin("java-base") {
    val toolchain = the<JavaPluginExtension>().toolchain
    val launcher = the<JavaToolchainService>().launcherFor(toolchain)
    tasks.withType<Detekt>().configureEach {
        jdkHome.convention(launcher.map { it.metadata.installationPath })
        jvmTarget = JavaVersion.toVersion(launcher.get().metadata.languageVersion.asInt()).toString()
    }
    tasks.withType<DetektCreateBaselineTask>().configureEach {
        jdkHome.convention(launcher.map { it.metadata.installationPath })
        jvmTarget = JavaVersion.toVersion(launcher.get().metadata.languageVersion.asInt()).toString()
    }
}

val configGeneration = gradle.sharedServices.registerIfAbsent(
    "StenoDetektConfigGeneration",
    DetektGenerateConfigTask.SingleExecutionBuildService::class.java,
) { maxParallelUsages.set(1) }
tasks.register<DetektGenerateConfigTask>("detektGenerateConfig") {
    detektClasspath.setFrom(engine)
    pluginClasspath.setFrom(rules)
    configFile.convention(layout.file(providers.provider { settings.config.last() }))
    usesService(configGeneration)
}
