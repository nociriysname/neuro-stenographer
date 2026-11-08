package ru.nuclearhack.stenographer.detekt

import io.github.detekt.test.utils.compileContentForTest
import io.gitlab.arturbosch.detekt.api.BaseRule
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.test.lint

internal fun BaseRule.lintAt(path: String, content: String): List<Finding> =
    lint(compileContentForTest(content.trimIndent(), path))

private const val UIKIT_SOURCES = "sources/common/common-uikit/src/main/kotlin/ru/nuclearhack/stenographer/common/uikit"

internal const val UIKIT_COMPONENT = "$UIKIT_SOURCES/components/StenoCard.kt"
internal const val UIKIT_THEME = "$UIKIT_SOURCES/foundation/theme/StenoColors.kt"
internal const val FEATURE_SCREEN =
    "sources/feature/feature-home/src/main/kotlin/ru/nuclearhack/stenographer/feature/home/presentation/HomeScreen.kt"
