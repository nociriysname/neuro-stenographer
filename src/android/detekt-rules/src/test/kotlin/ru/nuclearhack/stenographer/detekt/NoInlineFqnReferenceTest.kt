package ru.nuclearhack.stenographer.detekt

import io.gitlab.arturbosch.detekt.test.compileAndLint
import kotlin.test.Test
import kotlin.test.assertEquals

class NoInlineFqnReferenceTest {
    private val rule = NoInlineFqnReference()

    @Test
    fun `reports inline FQN in expression type and annotation`() {
        val findings = rule.compileAndLint(
            """
            @file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

            package sample

            fun value(size: androidx.compose.ui.unit.Dp): dev.zacsweers.metro.AppScope {
                return ru.nuclearhack.stenographer.Factory.create()
            }
            """.trimIndent(),
        )

        assertEquals(4, findings.size)
    }

    @Test
    fun `ignores package imports and imported short names`() {
        val findings = rule.compileAndLint(
            """
            package ru.nuclearhack.stenographer.sample

            import androidx.compose.ui.unit.Dp

            fun value(size: Dp): Dp = size
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }
}
