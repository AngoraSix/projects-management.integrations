package com.angorasix.projects.management.integrations.messaging.handler

import com.angorasix.commons.infrastructure.intercommunication.A6DomainResource
import com.angorasix.commons.infrastructure.intercommunication.A6InfraTopics
import com.angorasix.commons.infrastructure.intercommunication.integrations.IntegrationTaskReceived
import com.angorasix.commons.infrastructure.intercommunication.messaging.A6InfraMessageDto
import com.angorasix.commons.infrastructure.intercommunication.tasks.TasksSyncingCorrespondenceProcessed
import com.angorasix.projects.management.integrations.application.SourceSyncService
import kotlinx.coroutines.runBlocking

/**
 * <p>
 * </p>
 *
 * @author rozagerardo
 */
class ProjectsManagementIntegrationsMessagingHandler(
    private val sourceSyncService: SourceSyncService,
) {
    fun reprocessPendingAssets(message: A6InfraMessageDto<IntegrationTaskReceived>) =
        runBlocking {
            if (message.topic.any()) {
                error("Not re-processing at the moment - move to DLQ")
            }
        }

    fun processSyncingCorrespondence(message: A6InfraMessageDto<TasksSyncingCorrespondenceProcessed>) =
        runBlocking {
            if (message.topic == A6InfraTopics.TASKS_INTEGRATION_SYNCING_CORRESPONDENCE.value &&
                message.targetType == A6DomainResource.INTEGRATION_SOURCE_SYNC_EVENT
            ) {
                val correspondencesBulk = message.messageData
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
