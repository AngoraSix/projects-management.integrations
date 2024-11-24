package com.angorasix.projects.management.integrations.domain.integration.sourcesync

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.domain.inputs.InlineFieldSpec
import com.angorasix.commons.domain.projectmanagement.integrations.Source
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import java.time.Instant

/**
 * Source Sync Root.
 *
 * A syncing of data with the third-part integration.
 *
 * @author rozagerardo
 */
data class SourceSync @PersistenceCreator constructor(
    @field:Id val id: String?,
    val source: Source,
    val integrationId: String,
    val startedInstant: Instant,
    var lastInteractionInstant: Instant,
    var status: SourceSyncStatus,
    val admins: Set<SimpleContributor> = emptySet(),
    val sourceStrategyStateData: Any?, // any information used by the integration/source strategy to manage its state
) {
    constructor(
        source: Source,
        integrationId: String,
        startedDateTime: Instant,
        lastInteractionDateTime: Instant,
        status: SourceSyncStatus,
        admins: Set<SimpleContributor> = emptySet(),
        sourceStrategyStateData: Any?,
    ) : this(
        null,
        source,
        integrationId,
        startedDateTime,
        lastInteractionDateTime,
        status,
        admins,
        sourceStrategyStateData,
    )

    /**
     * Checks whether a particular contributor is Admin of this Club.
     *
     * @param contributorId - contributor candidate to check.
     */
    fun isAdmin(contributorId: String?): Boolean =
        (contributorId != null).and(admins.any { it.contributorId == contributorId })
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
