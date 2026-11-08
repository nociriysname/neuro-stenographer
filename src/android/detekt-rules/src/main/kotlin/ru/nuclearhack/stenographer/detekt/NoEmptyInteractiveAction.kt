package ru.nuclearhack.stenographer.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtValueArgument

class NoEmptyInteractiveAction(config: Config = Config.empty) : Rule(config) {

    override val issue: Issue get() = ISSUE
    override val active: Boolean get() = true

    override fun visitArgument(argument: KtValueArgument) {
        super.visitArgument(argument)
        if (argument.getArgumentName()?.asName?.asString() != ON_CLICK) return
        val lambda = argument.getArgumentExpression() as? KtLambdaExpression ?: return
        if (lambda.bodyExpression?.statements?.isNotEmpty() == true) return
        val path = argument.normalizedPath
        if (!path.isUikitModulePath()) return
        report(
            CodeSmell(
                issue,
                Entity.from(argument),
                "Пустой onClick создает ложное действие доступности; используйте блокировщик хит-тестов.",
            ),
        )
    }

    private companion object {
        private val ISSUE = Issue(
            id = "NoEmptyInteractiveAction",
            severity = Severity.Defect,
            description = "Продакшн UIKit не должен предоставлять пустые интерактивные действия.",
            debt = Debt.FIVE_MINS,
        )
        private const val ON_CLICK = "onClick"
    }
}
