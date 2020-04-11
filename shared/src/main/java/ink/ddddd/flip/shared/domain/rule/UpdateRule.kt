package ink.ddddd.flip.shared.domain.rule

import ink.ddddd.flip.shared.data.model.Rule
import ink.ddddd.flip.shared.data.source.RuleFilterDao
import ink.ddddd.flip.shared.domain.UseCase
import javax.inject.Inject

class UpdateRule @Inject constructor(
    private val ruleFilterDao: RuleFilterDao
) : UseCase<Rule, Unit>() {
    override fun execute(parameters: Rule) {
        ruleFilterDao.deleteFilterBeanByRule(parameters.id)
        ruleFilterDao.updateRule(parameters)
        parameters.filters.forEach {
            ruleFilterDao.updateFilterBean(it.toFilterBean())
        }
    }

}