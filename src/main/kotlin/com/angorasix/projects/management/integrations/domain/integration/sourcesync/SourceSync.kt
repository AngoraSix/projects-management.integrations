package com.angorasix.projects.management.integrations.domain.integration.sourcesync

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.domain.inputs.InlineFieldSpec
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAsset
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.util.UUID

/**
 * <p>
 *     Root entity defining the Project Management Source Sync Configuration data.
 * </p>
 *
 * @author rozagerardo
 */
@Document
@CompoundIndex(
    name = "sourcesync_idx",
    def = "{'source': 1, 'projectManagementId': 1}",
    unique = true,
)
data class SourceSync
    @PersistenceCreator
    constructor(
        @field:Id val id: String?,
        val source: String,
        val projectManagementId: String, // for a particular Project Mgmt (same user/admin could link to the same source),
        val admins: Set<SimpleContributor> = emptySet(),
        var status: SourceSyncStatus,
        val config: SourceSyncConfig = SourceSyncConfig(),
        val events: MutableList<SourceSyncEvent> = mutableListOf(),
        val mappings: SourceSyncMappings = SourceSyncMappings(),
    ) {
        @Transient
        var assets: List<IntegrationAsset> = emptyList()

        /**
         * Checks whether a particular contributor is Admin of this Club.
         *
         * @param contributorId - contributor candidate to check.
         */
        fun isAdmin(contributorId: String?): Boolean =
            (contributorId != null).and(
                admins.any {
                    it.contributorId ==
                        contributorId
                },
            )

        fun isActive(): Boolean = status.status == SourceSyncStatusValues.REGISTERED

        fun isInProgress(): Boolean = status.status == SourceSyncStatusValues.IN_PROGRESS

        private fun isDisabled(): Boolean = status.status == SourceSyncStatusValues.DISABLED

        fun addEvent(event: SourceSyncEvent) {
            events.add(event)
        }

        fun addStep(step: SourceSyncStatusStep) {
            status = SourceSyncStatus.inProgress()
            config.steps.add(step)
        }

        fun wasRequestedFullSync(): Boolean =
            isActive() &&
                events.last().type == SourceSyncEventValues.REQUEST_FULL_SYNC

        fun wasRequestedDisable(): Boolean =
            isDisabled() &&
                events.last().type == SourceSyncEventValues.REQUEST_UPDATE_STATE

        fun requiresFurtherConfiguration(): Boolean =
            isInProgress() &&
                config.steps.any { !it.isCompleted() }

        companion object {
            fun initiate(
                baseSourceSyncData: SourceSync,
                requestingContributor: SimpleContributor,
                config: SourceSyncConfig,
                id: String? = null,
            ): SourceSync =
                SourceSync(
                    id ?: baseSourceSyncData.id,
                    baseSourceSyncData.source,
                    baseSourceSyncData.projectManagementId,
                    setOf(requestingContributor),
                    SourceSyncStatus.inProgress(),
                    config,
                )

            fun notRegistered(
                source: String,
                projectManagementId: String,
                config: SourceSyncConfig? = null,
            ): SourceSync =
                SourceSync(
                    null,
                    source,
                    projectManagementId,
                    emptySet(),
                    SourceSyncStatus.notRegistered(),
                    config ?: SourceSyncConfig(),
                )
        }
    }

data class SourceSyncStatus(
    val status: SourceSyncStatusValues, // should match one of the IntegrationStatusValues, but flexible
    val expirationDate: Instant? = null, // the integration or syncing expiration date
//    val sourceStrategyStatusData: Map<String, Any>? = null, // any information used by the source to manage its stat
) {
    companion object {
        fun inProgress(): SourceSyncStatus = SourceSyncStatus(SourceSyncStatusValues.IN_PROGRESS)

        fun registered(): SourceSyncStatus = SourceSyncStatus(SourceSyncStatusValues.REGISTERED, Instant.now())

        fun notRegistered(): SourceSyncStatus = SourceSyncStatus(SourceSyncStatusValues.NOT_REGISTERED)
    }
}

enum class SourceSyncStatusValues {
    NOT_REGISTERED,
    IN_PROGRESS,
    REGISTERED,
    DISABLED,
}

data class SourceSyncConfig(
    var accessToken: String? = null,
    var sourceUserId: String? = null,
    val steps: MutableList<SourceSyncStatusStep> =
        mutableListOf(),
//        arrayListOf(),
)

data class SourceSyncEvent(
    val type: SourceSyncEventValues,
    val integrationEventId: String = UUID.randomUUID().toString(),
    val affectedQty: Int? = null,
    val eventInstant: Instant = Instant.now(),
)

enum class SourceSyncEventValues {
    INITIATED_CONFIG,
    REGISTERED_CONFIG,
    REQUEST_UPDATE_STATE,
    REQUEST_FULL_SYNC,
    TRIGGERED_FULL_SYNC,
    REQUEST_UPDATE_SYNC_CONFIG,
    FULL_SYNC_CORRESPONDENCE,
    STARTING_MEMBER_MATCH,
}

// data class SourceSyncStatus(
//    var status: SourceSyncStatusValues,
//    val steps: MutableList<SourceSyncStatusStep> = arrayListOf(),
// )

data class SourceSyncStatusStep(
    val stepKey: String,
    val requiredDataForStep: List<InlineFieldSpec> = emptyList(),
    var responseData: Map<String, List<String>>? = null,
) {
    fun isCompleted(): Boolean =
        responseData != null &&
            requiredDataForStep.all {
                responseData!!.containsKey(it.name)
            }
}

// enum class SourceSyncStatusValues {
//    IN_PROGRESS,
//    COMPLETED,
//    FAILED,
// }

data class SourceSyncMappings(
    val users: MutableMap<String, String?> = mutableMapOf(), // A6 Contributor id to Source User
) {
    fun addUserMappings(newUserMappings: Map<String, String?>) {
        users.putAll(newUserMappings)
    }

    fun addNewUserMappings(newUserMappings: Map<String, String?>) {
        users.putAllIfAbsent(newUserMappings)
    }

    fun getContributorsFromSources(sources: List<String>):Set<String> {
        return users.filterValues { sources.contains(it) }.keys
    }
}

private fun <K, V> MutableMap<K, V>.putAllIfAbsent(other: Map<K, V>) {
    for ((key, value) in other) {
        this.putIfAbsent(key, value)
    }
}
