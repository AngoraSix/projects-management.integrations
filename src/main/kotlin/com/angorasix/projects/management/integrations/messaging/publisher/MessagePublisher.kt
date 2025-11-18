package com.angorasix.projects.management.integrations.messaging.publisher

import com.angorasix.commons.domain.A6Contributor
import com.angorasix.commons.infrastructure.intercommunication.A6DomainResource
import com.angorasix.commons.infrastructure.intercommunication.A6InfraTopics
import com.angorasix.commons.infrastructure.intercommunication.integrations.IntegrationTaskReceived
import com.angorasix.commons.infrastructure.intercommunication.messaging.A6InfraMessageDto
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.amqp.AmqpConfigurations
import com.angorasix.projects.management.integrations.infrastructure.domain.SourceSyncContext
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.messaging.support.MessageBuilder

class MessagePublisher(
    private val streamBridge: StreamBridge,
    private val amqpConfigs: AmqpConfigurations,
) {
    fun publishAssetsUpdated(
        integrationTaskReceived: IntegrationTaskReceived,
        syncingEventId: String,
        sourceSyncContext: SourceSyncContext,
        requestingContributor: A6Contributor,
        areForPendingSyncing: Boolean = false,
    ) {
        val bindingKey: String =
            if (!areForPendingSyncing) {
                amqpConfigs.bindings.mgmtIntegrationSyncing
            } else {
                amqpConfigs.bindings.pendingSyncingOut
            }
        streamBridge.send(
            bindingKey,
            MessageBuilder
                .withPayload(
                    A6InfraMessageDto(
                        sourceSyncContext.projectManagementId,
                        A6DomainResource.PROJECT_MANAGEMENT,
                        "${sourceSyncContext.sourceSyncId}:$syncingEventId",
                        A6DomainResource.INTEGRATION_SOURCE_SYNC_EVENT.value,
                        A6InfraTopics.TASKS_INTEGRATION_FULL_SYNCING.value,
                        requestingContributor,
                        integrationTaskReceived,
                    ),
                ).build(),
        )
    }
}
