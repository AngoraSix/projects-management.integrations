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
        val updatedAssets = mutableListOf<IntegrationAsset>()
        val pendingUpdatedAssets =
            mutableListOf<IntegrationAsset>() // unsynced assets

        assets.forEach { asset ->
            val existing =
                existingSourceSyncAssets.find { existing -> existing.sourceData.id == asset.sourceData.id }
            updatedAssetOrNull(asset, existing)?.let {
                if (existing == null || existing.integrationStatus.status == IntegrationStatusValues.SYNCED) {
                    updatedAssets.add(it)
                } else {
                    pendingUpdatedAssets.add(it)
                }
            }
        }
        val persistedAssets = repository.saveAll(updatedAssets).toList()
        publishUpdatedAssets(
            persistedAssets,
            amqpConfigs.bindings.mgmtIntegrationSyncing,
            projectManagementId,
            sourceSyncId,
            requestingContributor,
        )
        repository.updateAllStatus(
            ListIntegrationAssetFilter(persistedAssets.mapNotNull { it.id }),
            IntegrationStatusValues.SYNCING_IN_PROGRESS,
        )

        publishUpdatedAssets(
            pendingUpdatedAssets,
            amqpConfigs.bindings.pendingSyncingOut,
            projectManagementId,
            sourceSyncId,
            requestingContributor,
        )
    }

    private suspend fun publishUpdatedAssets(
        updatedAssets: List<IntegrationAsset>,
        bindingKey: String,
        projectManagementId: String,
        sourceSyncId: String,
        requestingContributor: DetailedContributor,
    ) {
        if (updatedAssets.isNotEmpty()) {
            val messageData = A6InfraBulkResourceDto(
                A6DomainResource.Task,
                updatedAssets.map {
                    val sourceData = it.sourceData
                    A6InfraTaskDto(
                        sourceData.title,
                        sourceData.description,
                        sourceData.dueInstant,
                        emptySet(),
                        sourceData.done,
                        sourceData.id,
                        sourceData.type,
                    )
                },
            )
            streamBridge.send(
                bindingKey,
                MessageBuilder.withPayload(
                    A6InfraMessageDto(
                        projectManagementId,
                        A6DomainResource.ProjectManagement,
                        sourceSyncId,
                        A6DomainResource.IntegrationSourceSync.value,
                        A6InfraTopics.TASKS_INTEGRATION_FULL_SYNCING.value,
                        requestingContributor,
                        messageData.toMap(),
                    ),
                ).build(),
            )
        }
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
