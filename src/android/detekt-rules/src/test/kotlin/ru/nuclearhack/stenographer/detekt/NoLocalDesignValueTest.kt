package ru.nuclearhack.stenographer.detekt

import io.gitlab.arturbosch.detekt.test.compileAndLint
import kotlin.test.Test
import kotlin.test.assertEquals

class NoLocalDesignValueTest {
    private val rule = NoLocalDesignValue()

    @Test
    fun `reports size literals and hardcoded colors in features`() {
        val findings = rule.lintAt(
            FEATURE_SCREEN,
            """
            package sample

            val padding = 12.dp
            val text = 14.sp
            val brand = Color(0xFF00AAFF)
            val border = Color.Black
            """,
        )

        assertEquals(4, findings.size)
    }

    @Test
    fun `reports visual fractions but keeps math boundaries`() {
        val findings = rule.compileAndLint(
            """
            package sample

            fun layout(modifier: Modifier) {
                val zero = 0.dp
                modifier.weight(1f)
                modifier.weight(0.35f)
                modifier.fillMaxWidth(0.5f)
                modifier.alpha(0.6f)
            }
            """.trimIndent(),
        )

        assertEquals(2, findings.size)
    }

    @Test
    fun `ignores values taken from the theme and non visual numbers`() {
        val findings = rule.compileAndLint(
            """
            package sample

            const val TIMEOUT_SECONDS = 0.5
            fun content(modifier: Modifier) {
                modifier
                    .padding(StenoTheme.spacing.m)
                    .background(StenoTheme.colors.surface)
            }
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `uikit owns its literal values`() {
        val componentFindings = rule.lintAt(
            UIKIT_COMPONENT,
            """
            package ru.nuclearhack.stenographer.common.uikit.components

            val padding = 12.dp
            """,
        )
        val themeFindings = rule.lintAt(
            UIKIT_THEME,
            """
            package ru.nuclearhack.stenographer.common.uikit.foundation.theme

            val surface = Color(0xFF00AAFF)
            """,
        )

        assertEquals(0, componentFindings.size + themeFindings.size)
    }
}
