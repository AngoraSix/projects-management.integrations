package com.angorasix.projects.management.integrations.application

import com.angorasix.commons.domain.DetailedContributor
import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.domain.inputs.FieldSpec
import com.angorasix.commons.domain.inputs.InlineFieldOptions
import com.angorasix.commons.domain.inputs.InlineFieldSpec
import com.angorasix.commons.domain.inputs.OptionSpec
import com.angorasix.commons.domain.projectmanagement.integrations.Source
import com.angorasix.projects.management.integrations.application.strategies.SourceSyncStrategy
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSync
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncEvent
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncEventValues
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncRepository
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncStatus
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceUser
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.inputs.SourceSyncMappingsUsersInput
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.modification.SourceSyncModification
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.modification.SourceSyncOperation
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.SourceConfigurations
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.SourceSyncFilter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.util.UUID

/**
 *
 *
 * @author rozagerardo
 */
class SourceSyncService(
    private val repository: SourceSyncRepository,
    private val sourceConfigs: SourceConfigurations,
    private val sourceSyncStrategies: Map<Source, SourceSyncStrategy>,
    private val assetsService: IntegrationAssetService,
) {
    suspend fun findSingleSourceSync(
        id: String,
        requestingContributor: SimpleContributor,
        includeAssets: Boolean = false,
    ): SourceSync? =
        repository
            .findSingleUsingFilter(
                SourceSyncFilter(listOf(id)),
                requestingContributor,
            )?.apply {
                if (includeAssets) {
                    assets = assetsService.findForSourceSyncId(id).toList()
                }
            }

    fun findSourceSyncsForProjectManagement(
        projectManagementId: String,
        requestingContributor: SimpleContributor,
    ): List<SourceSync> =
        runBlocking {
            val filter = SourceSyncFilter(null, null, listOf(projectManagementId))
            val integrationList = repository.findUsingFilter(filter, requestingContributor).toList()
            sourceConfigs.supported.map { source ->
                integrationList
                    .find { it.source == source }
                    ?: SourceSync.notRegistered(source, projectManagementId)
            }
        }

    suspend fun registerSourceSync(
        newSourceSyncData: SourceSync,
        requestingContributor: SimpleContributor,
    ): SourceSync {
        val source = Source.valueOf(newSourceSyncData.source.uppercase())
        val existingSourceSync =
            repository.findSingleUsingFilter(
                SourceSyncFilter(
                    null,
                    setOf(source.value),
                    listOf(newSourceSyncData.projectManagementId),
                ),
                requestingContributor,
            )
        val sourceSyncRegistration =
            sourceSyncStrategies[source]
                ?.resolveSourceSyncRegistration(
                    newSourceSyncData,
                    requestingContributor,
                    existingSourceSync,
                )?.apply {
                    addEvent(
                        SourceSyncEvent(
                            SourceSyncEventValues.INITIATED_CONFIG,
                        ),
                    )
                } ?: throw IllegalArgumentException("Source not supported")
        return repository
            .save(sourceSyncRegistration)
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
        if (modificationOperations.size != 1) {
            throw IllegalArgumentException("Only one modification operation is allowed")
        }
        return repository
            .findSingleUsingFilter(
                SourceSyncFilter(listOf(sourceSyncId)),
                requestingContributor,
            )?.let {
                val patchedSourceSync =
                    modificationOperations.fold(it) { accumulatedSourceSync, op ->
                        op.modify(
                            requestingContributor,
                            accumulatedSourceSync,
                        )
                    }
                val source = it.source
                val sourceSyncStrategy =
                    sourceSyncStrategies[Source.valueOf(source.uppercase())]
                        ?: throw IllegalArgumentException(
                            "Source not supported for SourceSync operations: $source",
                        )
                val operation = modificationOperations.first().operation

                val updatedSourceSync =
                    postProcessPatchedSourceSync(operation, patchedSourceSync, sourceSyncStrategy, requestingContributor)
                repository.save(updatedSourceSync)
            }
    }

    private suspend fun SourceSyncService.postProcessPatchedSourceSync(
        operation: SourceSyncOperation,
        patchedSourceSync: SourceSync,
        sourceSyncStrategy: SourceSyncStrategy,
        requestingContributor: DetailedContributor,
    ) = when (operation) {
        SourceSyncOperation.MODIFY_STATUS -> patchedSourceSync

        SourceSyncOperation.REQUEST_FULL_SYNC_EVENT ->
            triggerFullSync(
                sourceSyncStrategy,
                patchedSourceSync,
                requestingContributor,
            )

        SourceSyncOperation.REPLACE_STEP_RESPONSE_DATA ->
            if (sourceSyncStrategy.isReadyForSyncing(
                    patchedSourceSync,
                    requestingContributor,
                )
            ) {
                triggerFullSync(
                    sourceSyncStrategy,
                    patchedSourceSync,
                    requestingContributor,
                ).apply {
                    status = SourceSyncStatus.registered()
                }
            } else {
                sourceSyncStrategy.configureNextStepData(
                    patchedSourceSync,
                    requestingContributor,
                )
            }

        SourceSyncOperation.REPLACE_MAPPING_USERS_DATA ->
            resendAssets(patchedSourceSync, requestingContributor)
    }

    private suspend fun resendAssets(patchedSourceSync: SourceSync, requestingContributor: DetailedContributor) : SourceSync {
        requireNotNull(patchedSourceSync.id) { "SourceSync id required for resendAssets" }
        val syncingEventId = UUID.randomUUID().toString()
        val assets = assetsService.findForSourceSyncId(patchedSourceSync.id).toList()
        assetsService.syncAssetsToTasks(
            assets,
            patchedSourceSync.projectManagementId,
            patchedSourceSync.id,
            syncingEventId,
            requestingContributor,
            patchedSourceSync.mappings,
        )

        return patchedSourceSync
    }

    private suspend fun triggerFullSync(
        sourceSyncStrategy: SourceSyncStrategy,
        patchedSourceSync: SourceSync,
        requestingContributor: DetailedContributor,
    ): SourceSync {
        requireNotNull(patchedSourceSync.id) { "SourceSync id required for triggerFullSync" }
        val syncEventId = UUID.randomUUID().toString()
        val assets =
            sourceSyncStrategy.triggerSourceSync(
                patchedSourceSync,
                requestingContributor,
                syncEventId,
            )
        val updatedAssets =
            assetsService.processAssets(
                assets,
                patchedSourceSync.id,
                patchedSourceSync.projectManagementId,
                requestingContributor,
                patchedSourceSync.mappings,
            )

        patchedSourceSync.addEvent(
            SourceSyncEvent(
                SourceSyncEventValues.TRIGGERED_FULL_SYNC,
                syncEventId,
                updatedAssets.size,
            ),
        )
        return patchedSourceSync
    }

    suspend fun processFullSyncCorrespondence(
        correspondences: List<Pair<String, String>>,
        sourceSyncId: String,
        syncingEventId: String,
        requestingContributor: DetailedContributor,
    ): SourceSync {
        val sourceSync =
            repository.findSingleUsingFilter(
                SourceSyncFilter((listOf(sourceSyncId))),
                requestingContributor,
            )
                ?: throw IllegalArgumentException(
                    "SourceSync [$sourceSyncId] not found for contributor",
                )

        assetsService.processSyncingCorrespondence(
            correspondences,
            sourceSyncId,
            syncingEventId,
        )

        sourceSync.addEvent(
            SourceSyncEvent(
                SourceSyncEventValues.FULL_SYNC_CORRESPONDENCE,
                syncingEventId,
                correspondences.size,
            ),
        )
        val updatedSourceSync = repository.save(sourceSync)
        return updatedSourceSync
    }

    suspend fun startUserMatching(
        contributorsToMatch: List<DetailedContributor>,
        sourceSyncId: String,
        requestingContributor: SimpleContributor,
    ): SourceSyncMappingsUsersInput {
        val sourceSync =
            repository.findSingleUsingFilter(
                SourceSyncFilter((listOf(sourceSyncId))),
                requestingContributor,
            )
        requireNotNull(sourceSync) { "SourceSync [$sourceSyncId] not found for contributor" }
        val sourceSyncStrategy = sourceSyncStrategies[Source.valueOf(sourceSync.source.uppercase())]
        requireNotNull(sourceSyncStrategy) {
            "Source not supported for SourceSync operations: ${sourceSync.source}"
        }

        val platformUsers =
            sourceSyncStrategy.obtainUsersMatchOptions(
                sourceSync,
                requestingContributor,
            )

        sourceSync.updateSourceSyncMappingUsers(contributorsToMatch)
        repository.save(sourceSync)
        return SourceSyncMappingsUsersInput(
            inputs =
                contributorsToMatch.map {
                    InlineFieldSpec(
                        name = it.contributorId,
                        type = FieldSpec.SELECT_COMPLEX,
                        options =
                            InlineFieldOptions(
                                selectedValues =
                                    determineSelectedValues(
                                        it,
                                        sourceSync.mappings.users,
                                        platformUsers,
                                    ),
                                inline =
                                    platformUsers.map { platformUser ->
                                        OptionSpec(
                                            value = platformUser.sourceUserId,
                                            prompt =
                                                platformUser.username
                                                    ?: platformUser.email
                                                    ?: platformUser.sourceUserId,
                                            promptData = platformUser.toMap(),
                                        )
                                    },
                            ),
                    )
                },
            source = sourceSync.source,
        )
    }

    private fun determineSelectedValues(
        contributor: DetailedContributor,
        existingMapping: Map<String, String?>,
        platformUsers: List<SourceUser>,
    ): List<String> {
        val existingValue = existingMapping[contributor.contributorId]
        val selectedValue =
            existingValue ?: platformUsers
                .takeIf { contributor.email != null }
                ?.find { it.email == contributor.email }
                ?.sourceUserId
        return selectedValue?.let { listOf(it) } ?: emptyList()
    }

    private fun SourceSync.updateSourceSyncMappingUsers(contributorsToMatch: List<DetailedContributor>) {
        val initialUsersMapping = contributorsToMatch.associate { it.contributorId to null }
        mappings.addNewUserMappings(initialUsersMapping)
        addEvent(
            SourceSyncEvent(
                type = SourceSyncEventValues.STARTING_MEMBER_MATCH,
                affectedQty = contributorsToMatch.size,
            ),
        )
    }
}
