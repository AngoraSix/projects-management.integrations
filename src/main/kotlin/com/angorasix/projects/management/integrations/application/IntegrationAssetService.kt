package com.angorasix.projects.management.integrations.application

import com.angorasix.commons.domain.A6Contributor
import com.angorasix.commons.infrastructure.intercommunication.A6DomainResource
import com.angorasix.commons.infrastructure.intercommunication.A6InfraTopics
import com.angorasix.commons.infrastructure.intercommunication.integrations.IntegrationTaskReceived
import com.angorasix.commons.infrastructure.intercommunication.messaging.A6InfraMessageDto
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAsset
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAssetRepository
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAssetStatus
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAssetSyncEvent
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationStatusValues
import com.angorasix.projects.management.integrations.domain.integration.asset.SourceAssetEstimationData
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSync
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.amqp.AmqpConfigurations
import com.angorasix.projects.management.integrations.infrastructure.domain.SourceSyncContext
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
    fun findForSourceSync(
        sourceSyncContext: SourceSyncContext,
        requestingContributor: A6Contributor,
    ) = repository.findUsingFilter(
        ListIntegrationAssetFilter(null, null, listOf(sourceSyncContext.sourceSyncId)),
        sourceSyncContext,
        requestingContributor,
    )

    /**
     * Method to modify [SourceSync].
     *
     */
    suspend fun processAssets(
        assets: List<IntegrationAsset>,
        sourceSyncContext: SourceSyncContext,
        requestingContributor: A6Contributor,
    ): List<IntegrationAsset> {
        val existingSourceSyncAssets =
            repository
                .findUsingFilter(
                    ListIntegrationAssetFilter(
                        null,
                        assets.map { it.sourceData.id },
                        listOf(sourceSyncContext.sourceSyncId),
                    ),
                    sourceSyncContext,
                    requestingContributor,
                ).toList()
        val updatedAssets = mutableListOf<IntegrationAsset>()
        val pendingUpdatedAssets = mutableListOf<IntegrationAsset>()

        assets.forEach { asset ->
            val existing =
                existingSourceSyncAssets.find { existing ->
                    existing.sourceData.id == asset.sourceData.id
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
            syncingEventId,
            sourceSyncContext,
            requestingContributor,
        )
        repository.registerEvent(
            ListIntegrationAssetFilter(persistedAssets.mapNotNull { it.id }),
            IntegrationAssetSyncEvent.syncing(syncingEventId),
            sourceSyncContext,
            requestingContributor,
        )

        // Updates not ready to be processed, for later
        publishUpdatedAssets(
            pendingUpdatedAssets,
            amqpConfigs.bindings.pendingSyncingOut,
            syncingEventId,
            sourceSyncContext,
            requestingContributor,
        )
        repository.registerEvent(
            ListIntegrationAssetFilter(pendingUpdatedAssets.mapNotNull { it.id }),
            IntegrationAssetSyncEvent.postponed(syncingEventId),
            sourceSyncContext,
            requestingContributor,
        )
        return persistedAssets
    }

    suspend fun syncAssets(
        assets: List<IntegrationAsset>,
        sourceSyncContext: SourceSyncContext,
        requestingContributor: A6Contributor,
    ) {
        val updatedAssets = mutableListOf<IntegrationAsset>()
        val pendingUpdatedAssets = mutableListOf<IntegrationAsset>()

        assets.forEach { asset ->
            if (asset.integrationAssetStatus.isSynced()) {
                updatedAssets.add(asset)
            } else {
                pendingUpdatedAssets.add(asset)
            }
        }

        val syncingEventId = UUID.randomUUID().toString()

        // Start Syncing
        publishUpdatedAssets(
            updatedAssets,
            amqpConfigs.bindings.mgmtIntegrationSyncing,
            syncingEventId,
            sourceSyncContext,
            requestingContributor,
        )
        repository.registerEvent(
            ListIntegrationAssetFilter(updatedAssets.mapNotNull { it.id }),
            IntegrationAssetSyncEvent.syncing(syncingEventId),
            sourceSyncContext,
            requestingContributor,
        )

        // Updates not ready to be processed, for later
        publishUpdatedAssets(
            pendingUpdatedAssets,
            amqpConfigs.bindings.pendingSyncingOut,
            syncingEventId,
            sourceSyncContext,
            requestingContributor,
        )
        repository.registerEvent(
            ListIntegrationAssetFilter(pendingUpdatedAssets.mapNotNull { it.id }),
            IntegrationAssetSyncEvent.postponed(syncingEventId),
            sourceSyncContext,
            requestingContributor,
        )
    }

    suspend fun processSyncingCorrespondence(
        correspondences: List<Pair<String, String>>,
        syncingEventId: String,
        sourceSyncContext: SourceSyncContext,
        requestingContributor: A6Contributor,
    ) {
        repository.registerCorrespondences(
            correspondences,
            syncingEventId,
            sourceSyncContext,
            requestingContributor,
        )
    }

    private fun publishUpdatedAssets(
        updatedAssets: List<IntegrationAsset>,
        bindingKey: String,
        syncingEventId: String,
        sourceSyncContext: SourceSyncContext,
        requestingContributor: A6Contributor,
    ) {
        if (updatedAssets.isNotEmpty()) {
            val messageData =
                IntegrationTaskReceived(
                    updatedAssets.map { it.toTaskDto(sourceSyncContext.configurations.usersMappings) },
                )
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
                            messageData,
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

private fun SourceAssetEstimationData.toDto(): IntegrationTaskReceived.IntegrationTaskEstimation =
    IntegrationTaskReceived.IntegrationTaskEstimation(
        caps,
        strategy,
        effort,
        complexity,
        industry,
        industryModifier,
        moneyPayment,
    )

private fun IntegrationAssetStatus.isSynced(): Boolean = currentStatus() == IntegrationStatusValues.SYNCED

private fun IntegrationAsset.toTaskDto(mappings: Map<String, String?>): IntegrationTaskReceived.IntegrationTask {
    requireNotNull(this.id)
    val sourceData = this.sourceData

    return IntegrationTaskReceived.IntegrationTask(
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
