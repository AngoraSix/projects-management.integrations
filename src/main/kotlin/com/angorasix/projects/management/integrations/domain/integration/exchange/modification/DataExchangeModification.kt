package com.angorasix.projects.management.integrations.domain.integration.exchange.modification

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.domain.modification.DomainObjectModification
import com.angorasix.projects.management.integrations.domain.integration.exchange.DataExchange

abstract class DataExchangeModification<U>(modifyValue: U) :
    DomainObjectModification<DataExchange, U>(modifyValue)

class ReplaceStepResponseData(stepResponses: List<Map<String, List<String>>?>) :
    DataExchangeModification<List<Map<String, List<String>>?>>(stepResponses) {
    override fun modify(
        simpleContributor: SimpleContributor,
        domainObject: DataExchange,
    ): DataExchange {
        require(domainObject.isAdmin(simpleContributor.contributorId)) { "Requesting contributor is not admin" }
        modifyValue.forEachIndexed { index, stepResponse ->
            if (stepResponse != null) {
                domainObject.status.steps[index].responseData = stepResponse
            }
        }
        return domainObject
    }
}
