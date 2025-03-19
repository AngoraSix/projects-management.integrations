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
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAssetStatus
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAssetSyncEvent
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationStatusValues
import com.angorasix.projects.management.integrations.domain.integration.asset.SourceAssetEstimationData
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSync
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.amqp.AmqpConfigurations
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.ListIntegrationAssetFilter
import kotlinx.coroutines.flow.toList
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.messaging.support.MessageBuilder
import java.util.UUID

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
    fun findForSourceSyncId(
        sourceSyncId: String,
    ) = repository.findUsingFilter(
        ListIntegrationAssetFilter(null, null, listOf(sourceSyncId)),
        allowAnonymous = true, // assets dont have information about admin
    )

    /**
     * Method to modify [SourceSync].
     *
     */
    suspend fun processAssets(
        assets: List<IntegrationAsset>,
        sourceSyncId: String,
        projectManagementId: String,
        requestingContributor: DetailedContributor,
        mappings: Map<String, String?>,
    ): List<IntegrationAsset> {
        val existingSourceSyncAssets =
            repository
                .findUsingFilter(
                    ListIntegrationAssetFilter(
                        null,
                        assets.map { it.sourceData.id },
                        listOf(sourceSyncId),
                    ),
                    allowAnonymous = true, // assets dont have information about admin
                ).toList()
        val updatedAssets = mutableListOf<IntegrationAsset>()
        val pendingUpdatedAssets =
            mutableListOf<IntegrationAsset>() // unsynced assets

        assets.forEach { asset ->
            val existing =
                existingSourceSyncAssets.find { existing ->
                    existing.sourceData.id ==
                        asset.sourceData.id
                }
            updatedAssetOrNull(asset, existing)?.let {
                if (existing == null || existing.integrationAssetStatus.isSynced()) {
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
            sourceSyncId,
            requestingContributor,
            mappings,
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
            mappings,
        )
        repository.registerEvent(
            ListIntegrationAssetFilter(pendingUpdatedAssets.mapNotNull { it.id }),
            IntegrationAssetSyncEvent.postponed(syncingEventId),
        )
        return persistedAssets
    }

    suspend fun syncAssets(
        assets: List<IntegrationAsset>,
        sourceSyncId: String,
        projectManagementId: String,
        requestingContributor: DetailedContributor,
        mappings: Map<String, String?>,
    ) {
        val syncingEventId = UUID.randomUUID().toString()

        publishUpdatedAssets(
            assets,
            amqpConfigs.bindings.mgmtIntegrationSyncing,
            projectManagementId,
            sourceSyncId,
            requestingContributor,
            mappings,
        )
        repository.registerEvent(
            ListIntegrationAssetFilter(assets.mapNotNull { it.id }),
            IntegrationAssetSyncEvent.syncing(syncingEventId),
        )
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
        mappings: Map<String, String?>,
    ) {
        if (updatedAssets.isNotEmpty()) {
            val messageData =
                A6InfraBulkResourceDto(
                    A6DomainResource.Task,
                    updatedAssets.map { it.toTaskDto(mappings) },
                )
            streamBridge.send(
                bindingKey,
                MessageBuilder
                    .withPayload(
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
): IntegrationAsset? =
    if (existing != null) {
        if (asset.requiresUpdate(existing)) {
            asset.copy(id = existing.id)
        } else {
            null
        }
    } else {
        asset
    }

private fun SourceAssetEstimationData.toDto(): A6InfraTaskEstimationDto =
    A6InfraTaskEstimationDto(
        caps,
        strategy,
        effort,
        complexity,
        industry,
        industryModifier,
        moneyPayment,
    )

private fun IntegrationAssetStatus.isSynced(): Boolean = currentStatus() == IntegrationStatusValues.SYNCED

private fun IntegrationAsset.toTaskDto(mappings: Map<String, String?>): A6InfraTaskDto {
    requireNotNull(this.id)
    val sourceData = this.sourceData

    return A6InfraTaskDto(
        this.id,
        sourceData.title,
        sourceData.description,
        sourceData.dueInstant,
        mappings.filterValues { sourceData.assigneeIds.contains(it) }.keys,
        sourceData.done,
        sourceData.type,
        sourceData.id,
        sourceData.estimations?.toDto(),
    )
}
