package ru.nuclearhack.stenographer.detekt

import kotlin.test.Test
import kotlin.test.assertEquals

class NoEmptyInteractiveActionTest {
    private val rule = NoEmptyInteractiveAction()

    @Test
    fun `reports empty onClick in uikit`() {
        val findings = rule.lintAt(
            UIKIT_COMPONENT,
            """
            package sample
            fun content() {
                Button(onClick = {})
            }
            """,
        )

        assertEquals(1, findings.size)
    }

    @Test
    fun `ignores features and non empty actions`() {
        val feature = rule.lintAt(FEATURE_SCREEN, "package sample\nfun content() { Button(onClick = {}) }")
        val uikit = rule.lintAt(
            UIKIT_COMPONENT,
            "package sample\nfun content(go: () -> Unit) { Button(onClick = { go() }) }"
        )

        assertEquals(0, feature.size + uikit.size)
    }
}
