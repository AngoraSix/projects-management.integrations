package com.angorasix.projects.management.integrations.domain.integration.sourcesync

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.domain.inputs.InlineFieldSpec
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.util.*

/**
 * Source Sync Root.
 *
 * A syncing of data with the third-part integration.
 *
 * @author rozagerardo
 */
@Document
data class SourceSync @PersistenceCreator constructor(
    @field:Id val id: String?,
    val source: String,
    @Indexed(unique = true) val integrationId: String,
    var status: SourceSyncStatus,
    val admins: Set<SimpleContributor> = emptySet(),
    val events: MutableList<SourceSyncEvent> = mutableListOf(),
    val sourceStrategyStateData: Any?, // any sate information used by the source strategy
) {
    constructor(
        source: String,
        integrationId: String,
        status: SourceSyncStatus,
        admins: Set<SimpleContributor> = emptySet(),
        events: MutableList<SourceSyncEvent> = mutableListOf(),
        sourceStrategyStateData: Any?,
    ) : this(
        null,
        source,
        integrationId,
        status,
        admins,
        events,
        sourceStrategyStateData,
    )

    /**
     * Checks whether a particular contributor is Admin of this Club.
     *
     * @param contributorId - contributor candidate to check.
     */
    fun isAdmin(contributorId: String?): Boolean =
        (contributorId != null).and(admins.any { it.contributorId == contributorId })

    fun addEvent(event: SourceSyncEvent) {
        events.add(event)
    }

    fun wasRequestedFullSync(): Boolean =
        status.status === SourceSyncStatusValues.COMPLETED &&
            events.last().type == SourceSyncEventValues.REQUEST_FULL_SYNC
}

data class SourceSyncEvent(
    val type: SourceSyncEventValues,
    val syncEventId: String = UUID.randomUUID().toString(),
    val correspondenceQty: Int? = null,
    val eventInstant: Instant = Instant.now(),
)

enum class SourceSyncEventValues {
    STARTING_FULL_SYNC_CONFIG,
    REQUEST_FULL_SYNC,
    TRIGGERED_FULL_SYNC,
    REQUEST_UPDATE_SYNC_CONFIG,
    FULL_SYNC_CORRESPONDENCE,
}

data class SourceSyncStatus(
    var status: SourceSyncStatusValues,
    val steps: MutableList<SourceSyncStatusStep> = arrayListOf(),
)

data class SourceSyncStatusStep(
    val stepKey: String,
    val requiredDataForStep: List<InlineFieldSpec> = emptyList(),
    var responseData: Map<String, List<String>>? = null,
) {
    fun isCompleted(): Boolean = responseData != null &&
        requiredDataForStep.all {
            responseData!!.containsKey(it.name)
        }
}

enum class SourceSyncStatusValues {
    IN_PROGRESS, COMPLETED, FAILED
}
