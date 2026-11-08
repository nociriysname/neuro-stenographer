package ru.nuclearhack.stenographer.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtValueArgument

class NoLocalDesignValue(config: Config = Config.empty) : Rule(config) {

    override val issue: Issue get() = ISSUE
    override val active: Boolean get() = true

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)
        if (expression.normalizedPath.isDesignValueOwnerPath()) return

        val selector = (expression.selectorExpression as? KtNameReferenceExpression)
            ?.getReferencedName()
            ?: return
        val receiver = expression.receiverExpression

        when {
            selector in DIMENSION_SELECTORS && receiver.isNonZeroLiteral() ->
                report(expression, "size literal '${expression.text}'")

            selector in RAW_COLOR_SELECTORS && receiver.text == COLOR ->
                report(expression, "raw color '${expression.text}'")
        }
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (expression.normalizedPath.isDesignValueOwnerPath()) return

        val callee = (expression.calleeExpression as? KtNameReferenceExpression)
            ?.getReferencedName()
            ?: return
        val arguments = expression.valueArguments

        if (callee == COLOR && arguments.any { it.isHexLiteral() }) {
            report(expression, "hardcoded ARGB literal")
        }
        if (callee in FRACTION_CALLS && arguments.any { it.isVisualFraction(callee) }) {
            report(expression, "fraction literal in '$callee()'")
        }
        arguments
            .filter { argument ->
                callee !in FRACTION_CALLS &&
                    argument.getArgumentName()?.asName?.asString() == ALPHA &&
                    argument.isVisualAlpha()
            }
            .forEach { report(it, "alpha literal in '$callee()'") }
    }

    private fun KtValueArgument.isHexLiteral(): Boolean =
        constant()?.text?.startsWith("0x", ignoreCase = true) == true

    private fun KtValueArgument.isVisualFraction(callee: String): Boolean {
        val value = constant()?.numericValue() ?: return false
        return value !in NORMALIZED_BOUNDARIES &&
            !(value == NORMALIZED_MIDPOINT && callee in MIDPOINT_FRACTION_CALLS)
    }

    private fun KtValueArgument.isVisualAlpha(): Boolean =
        constant()?.numericValue()?.let { value -> value !in NORMALIZED_BOUNDARIES } == true

    private fun KtValueArgument.constant(): KtConstantExpression? =
        getArgumentExpression() as? KtConstantExpression

    private fun KtExpression.isNonZeroLiteral(): Boolean =
        (this as? KtConstantExpression)?.numericValue()?.let { it != NORMALIZED_MIN } == true

    private fun KtConstantExpression.numericValue(): Double? = text
        .replace("_", "")
        .removeSuffix("f")
        .removeSuffix("F")
        .toDoubleOrNull()

    /** common-uikit и есть дизайн-система: размеры компонентов живут в самих компонентах. */
    private fun String.isDesignValueOwnerPath(): Boolean = isUikitModulePath()

    private fun report(element: KtExpression, detail: String) {
        report(CodeSmell(issue, Entity.from(element), message(detail)))
    }

    private fun report(element: KtValueArgument, detail: String) {
        report(CodeSmell(issue, Entity.from(element), message(detail)))
    }

    private fun message(detail: String): String =
        "Локальное значение дизайна: $detail. Возьмите его из StenoTheme или вынесите в компонент " +
            "common-uikit."

    private companion object {
        private val ISSUE = Issue(
            id = "NoLocalDesignValue",
            severity = Severity.Style,
            description = "Размеры, цвета и визуальные доли принадлежат дизайн-токенам, а не местам вызова.",
            debt = Debt.FIVE_MINS,
        )

        private const val COLOR = "Color"
        private const val ALPHA = "alpha"
        private const val NORMALIZED_MIN = 0.0
        private const val NORMALIZED_MIDPOINT = 0.5
        private const val NORMALIZED_MAX = 1.0

        private val DIMENSION_SELECTORS = setOf("dp", "sp")
        private val NORMALIZED_BOUNDARIES = setOf(NORMALIZED_MIN, NORMALIZED_MAX)

        private val RAW_COLOR_SELECTORS = setOf(
            "Black", "White", "Red", "Blue", "Green", "Gray", "Yellow", "Cyan", "Magenta",
        )

        private val FRACTION_CALLS = setOf(
            "weight",
            "alpha",
            "scale",
            "fillMaxWidth",
            "fillMaxHeight",
            "aspectRatio",
        )
        private val MIDPOINT_FRACTION_CALLS = setOf("fillMaxWidth", "fillMaxHeight")
    }
}
