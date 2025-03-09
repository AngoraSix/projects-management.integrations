package com.angorasix.projects.management.integrations.domain.integration.sourcesync.modification

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.domain.modification.DomainObjectModification
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSync
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncEvent
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncEventValues
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncStatus
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncStatusValues
import java.time.Instant

enum class SourceSyncOperation {
    MODIFY_STATUS,
    REPLACE_STEP_RESPONSE_DATA,
    REPLACE_MAPPING_USERS_DATA,
    REQUEST_FULL_SYNC_EVENT,
}

abstract class SourceSyncModification<U>(
    modifyValue: U,
) : DomainObjectModification<SourceSync, U>(modifyValue) {
    abstract val operation: SourceSyncOperation
}

class ModifySourceSyncStatus(
    status: SourceSyncStatusValues,
) : SourceSyncModification<SourceSyncStatusValues>(status) {
    override val operation = SourceSyncOperation.MODIFY_STATUS

    override fun modify(
        simpleContributor: SimpleContributor,
        domainObject: SourceSync,
    ): SourceSync {
        require(domainObject.isAdmin(simpleContributor.contributorId)) {
            "Requesting contributor is not admin"
        }
        domainObject.status =
            SourceSyncStatus(
                status = modifyValue,
                expirationDate = Instant.now(),
            )
        domainObject.addEvent(
            SourceSyncEvent(
                SourceSyncEventValues.REQUEST_UPDATE_STATE,
            ),
        )
        return domainObject
    }
}

class ReplaceStepResponseData(
    stepResponses: List<Map<String, List<String>>?>,
) : SourceSyncModification<List<Map<String, List<String>>?>>(stepResponses) {
    override val operation = SourceSyncOperation.REPLACE_STEP_RESPONSE_DATA

    override fun modify(
        simpleContributor: SimpleContributor,
        domainObject: SourceSync,
    ): SourceSync {
        require(domainObject.isAdmin(simpleContributor.contributorId)) {
            "Requesting contributor is not admin"
        }
        modifyValue.forEachIndexed { index, stepResponse ->
            if (stepResponse != null) {
                domainObject.config.steps[index].responseData = stepResponse
            }
        }
        return domainObject
    }
}

class ReplaceMappingUsersData(
    stepResponses: Map<String, String>,
) : SourceSyncModification<Map<String, String>>(stepResponses) {
    override val operation = SourceSyncOperation.REPLACE_MAPPING_USERS_DATA

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
    override val operation = SourceSyncOperation.REQUEST_FULL_SYNC_EVENT

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
