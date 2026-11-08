package ru.nuclearhack.stenographer.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

/** Свои правила проекта. Перенесены из набора `cu` CuApp те, что не завязаны на его граф модулей. */
class StenoRuleSetProvider : RuleSetProvider {

    override val ruleSetId: String = RULE_SET_ID

    override fun instance(config: Config): RuleSet = RuleSet(
        ruleSetId,
        listOf(
            NoInlineFqnReference(config),
            DetektRuleIssueAsProperty(config),
            NoLocalDesignValue(config),
            NoEmptyInteractiveAction(config),
        ),
    )

    companion object {
        const val RULE_SET_ID = "steno"
    }
}
