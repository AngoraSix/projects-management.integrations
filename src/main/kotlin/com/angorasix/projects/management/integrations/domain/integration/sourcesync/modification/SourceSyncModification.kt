package com.angorasix.projects.management.integrations.domain.integration.sourcesync.modification

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.domain.modification.DomainObjectModification
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSync

abstract class SourceSyncModification<U>(modifyValue: U) :
    DomainObjectModification<SourceSync, U>(modifyValue)

class ReplaceStepResponseData(stepResponses: List<Map<String, List<String>>?>) :
    SourceSyncModification<List<Map<String, List<String>>?>>(stepResponses) {
    override fun modify(
        simpleContributor: SimpleContributor,
        domainObject: SourceSync,
    ): SourceSync {
        require(domainObject.isAdmin(simpleContributor.contributorId)) { "Requesting contributor is not admin" }
        modifyValue.forEachIndexed { index, stepResponse ->
            if (stepResponse != null) {
                domainObject.status.steps[index].responseData = stepResponse
            }
        }
        return domainObject
    }
}
