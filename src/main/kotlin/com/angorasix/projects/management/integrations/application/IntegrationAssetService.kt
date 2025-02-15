package com.angorasix.projects.management.integrations.application

import com.angorasix.commons.domain.DetailedContributor
import com.angorasix.commons.infrastructure.intercommunication.dto.A6DomainResource
import com.angorasix.commons.infrastructure.intercommunication.dto.A6InfraTopics
import com.angorasix.commons.infrastructure.intercommunication.dto.domainresources.A6InfraBulkResourceDto
import com.angorasix.commons.infrastructure.intercommunication.dto.domainresources.A6InfraTaskDto
import com.angorasix.commons.infrastructure.intercommunication.dto.domainresources.A6InfraTaskEstimationDto
import com.angorasix.commons.infrastructure.intercommunication.dto.messaging.A6InfraMessageDto
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAsset
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAssetRepository
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAssetSyncEvent
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationStatusValues
import com.angorasix.projects.management.integrations.domain.integration.asset.SourceAssetEstimationData
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSync
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.amqp.AmqpConfigurations
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.ListIntegrationAssetFilter
import kotlinx.coroutines.flow.toList
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.messaging.support.MessageBuilder
import java.util.*

/**
 *
 *
 * @author rozagerardo
 */
class IntegrationAssetService(
    private val repository: IntegrationAssetRepository,
    private val streamBridge: StreamBridge,
    private val amqpConfigs: AmqpConfigurations,
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
    ): List<IntegrationAsset> {
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
                if (existing == null || existing.integrationStatus.currentStatus() == IntegrationStatusValues.SYNCED) {
                    updatedAssets.add(it)
                } else {
                    pendingUpdatedAssets.add(it)
                }
            }
        }
        val persistedAssets = repository.saveAll(updatedAssets).toList()

        val syncingEventId = UUID.randomUUID().toString()
        // Start Syncing
        publishUpdatedAssets(
            persistedAssets,
            amqpConfigs.bindings.mgmtIntegrationSyncing,
            projectManagementId,
            "$sourceSyncId:$syncingEventId",
            requestingContributor,
        )
        repository.registerEvent(
            ListIntegrationAssetFilter(persistedAssets.mapNotNull { it.id }),
            IntegrationAssetSyncEvent.syncing(syncingEventId),
        )

        // Updates not ready to be processed, for later
        publishUpdatedAssets(
            pendingUpdatedAssets,
            amqpConfigs.bindings.pendingSyncingOut,
            projectManagementId,
            sourceSyncId,
            requestingContributor,
        )
        repository.registerEvent(
            ListIntegrationAssetFilter(pendingUpdatedAssets.mapNotNull { it.id }),
            IntegrationAssetSyncEvent.postponed(syncingEventId),
        )
        return persistedAssets
    }

    suspend fun processSyncingCorrespondence(
        correspondences: List<Pair<String, String>>,
        sourceSyncId: String,
        syncingEventId: String,
    ) {
        repository.registerCorrespondences(
            correspondences,
            sourceSyncId,
            syncingEventId,
        )
    }

    private fun publishUpdatedAssets(
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
                    requireNotNull(it.id)
                    val sourceData = it.sourceData
                    A6InfraTaskDto(
                        it.id,
                        sourceData.title,
                        sourceData.description,
                        sourceData.dueInstant,
                        emptySet(),
                        sourceData.done,
                        sourceData.type,
                        sourceData.id,
                        sourceData.estimations?.toDto(),
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

private fun SourceAssetEstimationData.toDto(): A6InfraTaskEstimationDto {
    return A6InfraTaskEstimationDto(
        caps,
        strategy,
        effort,
        complexity,
        industry,
        industryModifier,
        moneyPayment,
    )
}
