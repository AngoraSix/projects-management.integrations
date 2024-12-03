package com.angorasix.projects.management.integrations.messaging.handler

import com.angorasix.commons.infrastructure.intercommunication.dto.A6DomainResource
import com.angorasix.commons.infrastructure.intercommunication.dto.A6InfraTopics
import com.angorasix.commons.infrastructure.intercommunication.dto.messaging.A6InfraMessageDto
import com.angorasix.commons.infrastructure.intercommunication.dto.syncing.A6InfraBulkSyncingCorrespondenceDto
import com.angorasix.projects.management.integrations.application.SourceSyncService
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking

/**
 * <p>
 * </p>
 *
 * @author rozagerardo
 */
class ProjectsManagementIntegrationsMessagingHandler(
    private val sourceSyncService: SourceSyncService,
    private val objectMapper: ObjectMapper,
) {

    fun reprocessPendingAssets(message: A6InfraMessageDto) = runBlocking {
        if (message.topic.any()) {
            error("Not re-processing at the moment - move to DLQ")
        }
    }

    fun processSyncingCorrespondence(message: A6InfraMessageDto) = runBlocking {
        if (message.topic == A6InfraTopics.TASKS_INTEGRATION_SYNCING_CORRESPONDENCE.value &&
            message.targetType == A6DomainResource.IntegrationSourceSync
        ) {
            val correspondencesBulkJson = objectMapper.writeValueAsString(message.messageData)
            val correspondencesBulk = objectMapper.readValue(
                correspondencesBulkJson,
                A6InfraBulkSyncingCorrespondenceDto::class.java,
            )

            val correspondences =
                correspondencesBulk.collection.map { Pair(it.integrationId, it.a6Id) }
            val (sourceSyncId, syncingEventId) = message.targetId.split(":")
            sourceSyncService.processFullSyncCorrespondence(
                correspondences,
                sourceSyncId,
                syncingEventId,
                message.requestingContributor,
            )
        }
    }
}
