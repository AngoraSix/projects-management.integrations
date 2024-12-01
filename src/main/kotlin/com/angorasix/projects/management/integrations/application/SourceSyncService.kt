package com.angorasix.projects.management.integrations.application

import com.angorasix.commons.domain.DetailedContributor
import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.domain.projectmanagement.integrations.Source
import com.angorasix.projects.management.integrations.application.strategies.SourceSyncStrategy
import com.angorasix.projects.management.integrations.domain.integration.configuration.Integration
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSync
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncEvent
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncEventValues
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncRepository
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncStatusValues
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.modification.SourceSyncModification
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.ListSourceSyncFilter
import kotlinx.coroutines.flow.toList
import java.time.Instant

/**
 *
 *
 * @author rozagerardo
 */
class SourceSyncService(
    private val repository: SourceSyncRepository,
    private val integrationsService: IntegrationsService,
    private val sourceSyncStrategies: Map<Source, SourceSyncStrategy>,
    private val assetsService: IntegrationAssetService,
) {
    suspend fun createSourceSync(
        integrationId: String,
        requestingContributor: SimpleContributor,
    ): SourceSync? {
        val existingSourceSyncList = repository.findUsingFilter(
            ListSourceSyncFilter(null, listOf(integrationId)),
        ).toList()

        if (existingSourceSyncList.isNotEmpty() &&
            existingSourceSyncList.first().status.status == SourceSyncStatusValues.COMPLETED
        ) {
            throw IllegalArgumentException(
                "There is already a Completed SourceSync " +
                    "for integration [$integrationId]",
            )
        }
        val integration =
            integrationsService.findSingleIntegration(integrationId, requestingContributor)
        return integration?.let {
            val source = Source.valueOf(integration.source.uppercase())
            val sourceSync = sourceSyncStrategies[source]?.configSourceSync(
                integration,
                requestingContributor,
                existingSourceSyncList.firstOrNull(),
            )
            sourceSync?.let { repository.save(it) }
        }
    }

    /**
     * Method to modify [SourceSync].
     *
     */
    suspend fun modifySourceSync(
        requestingContributor: DetailedContributor,
        sourceSyncId: String,
        modificationOperations: List<SourceSyncModification<out Any>>,
    ): SourceSync? {
        val persistedSourceSync = repository.findForContributorUsingFilter(
            ListSourceSyncFilter(listOf(sourceSyncId)),
            requestingContributor,
        )
        return persistedSourceSync?.let {
            val patchedSourceSync =
                modificationOperations.fold(it) { accumulatedSourceSync, op ->
                    op.modify(
                        requestingContributor,
                        accumulatedSourceSync,
                    )
                }
            val source = persistedSourceSync.source
            val sourceSyncStrategy = sourceSyncStrategies[Source.valueOf(source.uppercase())]
                ?: throw IllegalArgumentException("Source not supported for SourceSync operations: $source")
            val integration = integrationsService.findSingleIntegration(
                persistedSourceSync.integrationId,
                requestingContributor,
            )
                ?: throw IllegalArgumentException(
                    "Couldn't find associated integration" +
                        "[${persistedSourceSync.integrationId}] for sourceSync [$sourceSyncId] }",
                )
            val updatedSourceSync =
                if (patchedSourceSync.wasRequestedFullSync() || sourceSyncStrategy.isReadyForSyncing(
                        patchedSourceSync,
                        integration,
                        requestingContributor,
                    )
                ) {
                    triggerFullSync(
                        sourceSyncStrategy,
                        patchedSourceSync,
                        integration,
                        requestingContributor,
                        sourceSyncId,
                    )
                } else {
                    sourceSyncStrategy.processModification(
                        patchedSourceSync,
                        integration,
                        requestingContributor,
                    )
                }
            repository.save(updatedSourceSync)
        }
    }

    private suspend fun triggerFullSync(
        sourceSyncStrategy: SourceSyncStrategy,
        patchedSourceSync: SourceSync,
        integration: Integration,
        requestingContributor: DetailedContributor,
        sourceSyncId: String,
    ): SourceSync {
        val assets = sourceSyncStrategy.triggerSourceSync(
            patchedSourceSync,
            integration,
            requestingContributor,
        )
        assetsService.processAssets(
            assets,
            sourceSyncId,
            integration.projectManagementId,
            requestingContributor,
        )

        patchedSourceSync.status.status = SourceSyncStatusValues.COMPLETED
        patchedSourceSync.addEvent(
            SourceSyncEvent(
                SourceSyncEventValues.TRIGGERED_FULL_SYNC,
                Instant.now(),
            ),
        )
        return patchedSourceSync
    }
}
