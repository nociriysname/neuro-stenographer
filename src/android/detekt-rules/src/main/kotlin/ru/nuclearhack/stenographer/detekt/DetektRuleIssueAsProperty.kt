package ru.nuclearhack.stenographer.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtProperty

class DetektRuleIssueAsProperty(config: Config = Config.empty) : Rule(config) {

    override val issue: Issue get() = ISSUE
    override val active: Boolean get() = true

    override fun visitProperty(property: KtProperty) {
        super.visitProperty(property)

        if (!property.hasModifier(KtTokens.OVERRIDE_KEYWORD)) return
        if (property.nameAsName?.asString() != "issue") return
        if (property.typeReference?.text != "Issue") return
        if (property.initializer == null) return

        report(
            CodeSmell(
                issue,
                Entity.from(property),
                "Используйте 'override val issue: Issue get() = ISSUE' (с константой в companion) " +
                    "вместо инициализатора свойства — суперконструктор считывает issue до инициализации подкласса.",
            ),
        )
    }

    private companion object {
        private val ISSUE = Issue(
            id = "DetektRuleIssueAsProperty",
            severity = Severity.Defect,
            description = "Инициализация 'issue' как свойства в Rule detekt вызывает NPE во время загрузки.",
            debt = Debt.FIVE_MINS,
        )
    }
}
