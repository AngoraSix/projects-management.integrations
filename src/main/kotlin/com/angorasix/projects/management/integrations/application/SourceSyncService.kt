package com.angorasix.projects.management.integrations.application

import com.angorasix.commons.domain.A6Contributor
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
import com.angorasix.projects.management.integrations.infrastructure.constants.ManagementIntegrationConstants
import com.angorasix.projects.management.integrations.infrastructure.domain.SourceSyncContext.Companion.context
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
        requestingContributor: A6Contributor,
    ): SourceSync? =
        repository
            .findSingleUsingFilter(
                SourceSyncFilter(listOf(id)),
                requestingContributor,
            )

    fun findSourceSyncsForProjectManagement(
        projectManagementId: String,
        requestingContributor: A6Contributor,
    ): List<SourceSync> =
        runBlocking {
            val filter = SourceSyncFilter(null, null, listOf(projectManagementId))
            val integrationList = repository.findUsingFilter(filter, requestingContributor).toList()
            sourceConfigs.supported.map { source ->
                integrationList.find { it.source == source } ?: SourceSync.notRegistered(
                    source,
                    projectManagementId,
                )
            }
        }

    suspend fun registerSourceSync(
        newSourceSyncData: SourceSync,
        requestingContributor: A6Contributor,
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
        return repository.save(sourceSyncRegistration)
    }

    /**
     * Method to modify [SourceSync].
     *
     */
    suspend fun modifySourceSync(
        requestingContributor: A6Contributor,
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
                    postProcessPatchedSourceSync(
                        operation,
                        patchedSourceSync,
                        sourceSyncStrategy,
                        requestingContributor,
                    )
                repository.save(updatedSourceSync)
            }
    }

    private suspend fun SourceSyncService.postProcessPatchedSourceSync(
        operation: SourceSyncOperation,
        patchedSourceSync: SourceSync,
        sourceSyncStrategy: SourceSyncStrategy,
        requestingContributor: A6Contributor,
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

        SourceSyncOperation.REPLACE_MAPPING_USERS_DATA -> {
            val sourceSyncContext = patchedSourceSync.context()
            val assets =
                assetsService
                    .findForSourceSync(
                        sourceSyncContext,
                        requestingContributor,
                    ).toList()

            assetsService.syncAssets(
                assets,
                sourceSyncContext,
                requestingContributor,
            )

            patchedSourceSync
        }
    }

    private suspend fun triggerFullSync(
        sourceSyncStrategy: SourceSyncStrategy,
        patchedSourceSync: SourceSync,
        requestingContributor: A6Contributor,
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
                patchedSourceSync.context(),
                requestingContributor,
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
        requestingContributor: A6Contributor,
    ): SourceSync {
        val sourceSync =
            repository.findSingleUsingFilter(
                SourceSyncFilter((listOf(sourceSyncId))),
                requestingContributor,
            ) ?: throw IllegalArgumentException(
                "SourceSync [$sourceSyncId] not found for contributor",
            )

        assetsService.processSyncingCorrespondence(
            correspondences,
            syncingEventId,
            sourceSync.context(),
            requestingContributor,
        )

        sourceSync.addEvent(
            SourceSyncEvent(
                SourceSyncEventValues.SYNC_CORRESPONDENCE,
                syncingEventId,
                correspondences.size,
            ),
        )
        val updatedSourceSync = repository.save(sourceSync)
        return updatedSourceSync
    }

    suspend fun startUserMatching(
        contributorsToMatch: List<A6Contributor>,
        sourceSyncId: String,
        requestingContributor: A6Contributor,
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
        sourceSync.updateSourceSyncMappingUsers(platformUsers)
        repository.save(sourceSync)
        return SourceSyncMappingsUsersInput(
            inputs =
                platformUsers.map {
                    InlineFieldSpec(
                        name = it.sourceUserId,
                        type = FieldSpec.SELECT_COMPLEX,
                        options =
                            InlineFieldOptions(
                                selectedValues =
                                    determineSelectedValues(
                                        it,
                                        sourceSync.mappings.users,
                                        contributorsToMatch,
                                    ),
                                inline =
                                    buildList {
                                        add(
                                            OptionSpec(
                                                ManagementIntegrationConstants.UNASSIGNED_KEY,
                                                ManagementIntegrationConstants.UNASSIGNED_KEY,
                                            ),
                                        )
                                        contributorsToMatch.forEach { contributor ->
                                            add(
                                                OptionSpec(
                                                    value = contributor.contributorId,
                                                    prompt = contributor.email ?: contributor.contributorId,
                                                ),
                                            )
                                        }
                                    },
                            ),
                        prompt =
                            it.username ?: it.email
                                ?: it.sourceUserId,
                        promptData = it.toMap(),
                    )
                },
            source = sourceSync.source,
        )
    }

    private fun determineSelectedValues(
        sourceUser: SourceUser,
        existingMapping: Map<String, String?>,
        contributorsToMatch: List<A6Contributor>,
    ): List<String> {
        val existingValue = existingMapping[sourceUser.sourceUserId]
        val selectedValue =
            existingValue ?: contributorsToMatch
                .takeIf { sourceUser.email != null }
                ?.find { it.email == sourceUser.email }
                ?.contributorId
        return selectedValue?.let { listOf(it) } ?: emptyList()
    }

    private fun SourceSync.updateSourceSyncMappingUsers(sourceUsersToMatch: Set<SourceUser>) {
        val initialUsersMapping = sourceUsersToMatch.associate { it.sourceUserId to null }
        mappings.addNewUserMappings(initialUsersMapping)
        addEvent(
            SourceSyncEvent(
                type = SourceSyncEventValues.STARTING_MEMBER_MATCH,
                affectedQty = sourceUsersToMatch.size,
            ),
        )
    }
}
