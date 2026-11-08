package ru.nuclearhack.stenographer.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

class NoInlineFqnReference(config: Config = Config.empty) : Rule(config) {

    override val issue: Issue get() = ISSUE
    override val active: Boolean get() = true

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)

        if (expression.parent is KtDotQualifiedExpression) return
        if (expression.getParentOfType<KtImportDirective>(strict = true) != null) return
        if (expression.getParentOfType<KtPackageDirective>(strict = true) != null) return
        val text = expression.text ?: return
        reportIfFqn(text.substringBefore('\n'), Entity.from(expression))
    }

    override fun visitUserType(type: KtUserType) {
        super.visitUserType(type)

        if (type.parent is KtUserType) return
        if (type.getParentOfType<KtImportDirective>(strict = true) != null) return
        if (type.getParentOfType<KtPackageDirective>(strict = true) != null) return

        reportIfFqn(type.text, Entity.from(type))
    }

    private fun reportIfFqn(text: String, entity: Entity) {
        if (!PATTERN.containsMatchIn(text)) return
        report(
            CodeSmell(
                issue,
                entity,
                "Обнаружена инлайновая ссылка FQN: '$text'. Используйте импорт.",
            ),
        )
    }

    private companion object {
        private val ISSUE = Issue(
            id = "NoInlineFqnReference",
            severity = Severity.Style,
            description = "Инлайновые полнотекстовые ссылки (FQN) запрещены; вынесите путь в импорт.",
            debt = Debt.FIVE_MINS,
        )

        private val PATTERN = Regex(
            """^(androidx|kotlinx|kotlin|android|java|ru\.nuclearhack|org\.jetbrains|""" +
                """dev\.zacsweers|com\.k2fsa|io\.gitlab)""" +
                """(\.[a-z][A-Za-z0-9_]*)*\.[A-Za-z_][A-Za-z0-9_]*""",
        )
    }
}
