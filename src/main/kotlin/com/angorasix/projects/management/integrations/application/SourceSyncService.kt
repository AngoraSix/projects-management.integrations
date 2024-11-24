package com.angorasix.projects.management.integrations.application

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.domain.projectmanagement.integrations.Source
import com.angorasix.projects.management.integrations.application.strategies.SourceSyncStrategy
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSync
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncRepository
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncStatusValues
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.modification.SourceSyncModification
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.ListSourceSyncFilter

/**
 *
 *
 * @author rozagerardo
 */
class SourceSyncService(
    private val repository: SourceSyncRepository,
    private val integrationsService: IntegrationsService,
    private val sourceSyncStrategies: Map<Source, SourceSyncStrategy>,
) {
    suspend fun createSourceSync(
        integrationId: String,
        requestingContributor: SimpleContributor,
    ): SourceSync? {
        val integration =
            integrationsService.findSingleIntegration(integrationId, requestingContributor)
        return integration?.let {
            val source = Source.valueOf(integration.source.uppercase())
            val sourceSync = sourceSyncStrategies[source]?.startSourceSync(
                integration,
                requestingContributor,
            )
            sourceSync?.let { repository.save(it) }
        }
    }

    /**
     * Method to modify [SourceSync].
     *
     */
    suspend fun modifySourceSync(
        requestingContributor: SimpleContributor,
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
            val sourceSyncStrategy = sourceSyncStrategies[source]
                ?: throw IllegalArgumentException("Source not supported for SourceSync operations: $source")
            val integration = integrationsService.findSingleIntegration(
                persistedSourceSync.integrationId,
                requestingContributor,
            )
                ?: throw IllegalArgumentException(
                    "Couldn't find associated integration" +
                        "[${persistedSourceSync.integrationId}] for sourceSync [$sourceSyncId] }",
                )

            val updatedSourceSync = if (sourceSyncStrategy.isReadyForSyncing(
                    patchedSourceSync,
                    integration,
                    requestingContributor,
                )
            ) {
//                val assets = sourceSyncStrategy.sourceSync(
//                    patchedSourceSync,
//                    integration,
//                    requestingContributor,
//                )
                // persist assets (created)
                // send assets to message queue (esto con event / domain event)
                // update assets status (waiting sync) (esto con post-processing domain event)
                // Maybe as Domain Event / AOP?
                patchedSourceSync.status.status = SourceSyncStatusValues.COMPLETED
                patchedSourceSync
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
}