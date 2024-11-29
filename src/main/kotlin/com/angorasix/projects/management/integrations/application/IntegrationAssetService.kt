package com.angorasix.projects.management.integrations.application

import com.angorasix.commons.domain.DetailedContributor
import com.angorasix.commons.infrastructure.intercommunication.dto.A6DomainResource
import com.angorasix.commons.infrastructure.intercommunication.dto.A6InfraTopics
import com.angorasix.commons.infrastructure.intercommunication.dto.domainresources.A6InfraBulkResourceDto
import com.angorasix.commons.infrastructure.intercommunication.dto.domainresources.A6InfraTaskDto
import com.angorasix.commons.infrastructure.intercommunication.dto.messaging.A6InfraMessageDto
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAsset
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAssetRepository
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationStatusValues
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSync
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.amqp.AmqpConfigs
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.ListIntegrationAssetFilter
import kotlinx.coroutines.flow.toList
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.messaging.support.MessageBuilder

/**
 *
 *
 * @author rozagerardo
 */
class IntegrationAssetService(
    private val repository: IntegrationAssetRepository,
    private val streamBridge: StreamBridge,
    private val amqpConfigs: AmqpConfigs,
) {

    /**
     * Method to modify [SourceSync].
     *
     */
    suspend fun processAssets(
        assets: List<IntegrationAsset>,
        sourceSyncId: String,
        projectManagementId: String,
        requestingContributor: DetailedContributor,
    ) {
        val existingSourceSyncAssets = repository.findUsingFilter(
            ListIntegrationAssetFilter(
                null,
                assets.map { it.sourceData.id },
                listOf(sourceSyncId),
            ),
        ).toList()
        val updatedAssets = assets.mapNotNull {
            updatedAssetOrNull(
                it,
                existingSourceSyncAssets.find { existing -> existing.sourceData.id == it.sourceData.id },
            )
        }
        val persistedAssets = repository.saveAll(updatedAssets).toList()

        val messageData = A6InfraBulkResourceDto(
            A6DomainResource.Task,
            persistedAssets.map {
                val sourceData = it.sourceData
                A6InfraTaskDto(
                    sourceData.id,
                    sourceData.type,
                    sourceData.title,
                    sourceData.description,
                    sourceData.dueInstant,
                    emptyList(),
                    sourceData.done,
                )
            },
        )
        streamBridge.send(
            amqpConfigs.bindings.sourceSyncingInwards,
            MessageBuilder.withPayload(
                A6InfraMessageDto(
                    projectManagementId,
                    A6DomainResource.ProjectManagement,
                    sourceSyncId,
                    A6DomainResource.IntegrationSourceSync.value,
                    A6InfraTopics.INTEGRATION_FULL_SYNCING.value,
                    requestingContributor,
                    messageData.toMap(),
                ),
            ).build(),
        )
        repository.updateAllStatus(
            ListIntegrationAssetFilter(persistedAssets.mapNotNull { it.id }),
            IntegrationStatusValues.SYNCING_IN_PROGRESS,
        )
    }
}

private fun updatedAssetOrNull(
    asset: IntegrationAsset,
    existing: IntegrationAsset?,
): IntegrationAsset? {
    return if (existing != null) {
        if (asset.requiresUpdate(existing)) {
            asset.copy(id = existing.id)
        } else {
            null
        }
    } else {
        asset
    }
}
