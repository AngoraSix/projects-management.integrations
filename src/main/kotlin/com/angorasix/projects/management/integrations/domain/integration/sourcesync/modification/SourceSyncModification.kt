package com.angorasix.projects.management.integrations.domain.integration.sourcesync.modification

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.domain.modification.DomainObjectModification
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSync
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncEvent
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncEventValues
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncStatusValues

abstract class SourceSyncModification<U>(
    modifyValue: U,
) : DomainObjectModification<SourceSync, U>(modifyValue)

class ReplaceStepResponseData(
    stepResponses: List<Map<String, List<String>>?>,
) : SourceSyncModification<List<Map<String, List<String>>?>>(stepResponses) {
    override fun modify(
        simpleContributor: SimpleContributor,
        domainObject: SourceSync,
    ): SourceSync {
        require(domainObject.isAdmin(simpleContributor.contributorId)) {
            "Requesting contributor is not admin"
        }
        modifyValue.forEachIndexed { index, stepResponse ->
            if (stepResponse != null) {
                domainObject.status.steps[index].responseData = stepResponse
            }
        }
        return domainObject
    }
}

class ReplaceMappingUsersData(
    stepResponses: Map<String, String>,
) : SourceSyncModification<Map<String, String>>(stepResponses) {
    override fun modify(
        simpleContributor: SimpleContributor,
        domainObject: SourceSync,
    ): SourceSync {
        require(domainObject.isAdmin(simpleContributor.contributorId)) {
            "Requesting contributor is not admin"
        }
        domainObject.mappings.addUserMappings(modifyValue)
        return domainObject
    }
}

class RequestFullSyncEvent(
    newEvent: SourceSyncEvent,
) : SourceSyncModification<SourceSyncEvent>(newEvent) {
    override fun modify(
        simpleContributor: SimpleContributor,
        domainObject: SourceSync,
    ): SourceSync {
        require(domainObject.isAdmin(simpleContributor.contributorId)) {
            "Requesting contributor is not admin"
        }
        if (modifyValue.type === SourceSyncEventValues.REQUEST_FULL_SYNC) {
            domainObject.addEvent(modifyValue)
        }
        return domainObject
    }
}

class RequestSyncConfigUpdate(
    status: SourceSyncStatusValues,
) : SourceSyncModification<SourceSyncStatusValues>(status) {
    override fun modify(
        simpleContributor: SimpleContributor,
        domainObject: SourceSync,
    ): SourceSync {
        require(domainObject.isAdmin(simpleContributor.contributorId)) {
            "Requesting contributor is not admin"
        }
        if (modifyValue === SourceSyncStatusValues.IN_PROGRESS) {
            domainObject.addEvent(
                SourceSyncEvent(
                    SourceSyncEventValues.REQUEST_UPDATE_SYNC_CONFIG,
                ),
            )
            domainObject.status.status = modifyValue
        }
        return domainObject
    }
}
