package com.angorasix.projects.management.integrations.domain.integration.exchange

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.domain.inputs.InlineFieldSpec
import com.angorasix.commons.domain.projectmanagement.integrations.Source
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import java.time.Instant

/**
 * Data Exchange Root.
 *
 * An exchange of data with the third-part integration.
 *
 * @author rozagerardo
 */
data class DataExchange @PersistenceCreator constructor(
    @field:Id val id: String?,
    val source: Source,
    val integrationId: String,
    val startedInstant: Instant,
    var lastInteractionInstant: Instant,
    var status: DataExchangeStatus,
    val admins: Set<SimpleContributor> = emptySet(),
    val sourceStrategyStateData: Any?, // any information used by the integration/source strategy to manage its state
) {
    constructor(
        integrationId: String,
        source: Source,
        startedDateTime: Instant,
        lastInteractionDateTime: Instant,
        status: DataExchangeStatus,
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

data class DataExchangeStatus(
    val status: DataExchangeStatusValues,
    val steps: List<DataExchangeStatusStep> = emptyList(),
)

data class DataExchangeStatusStep(
    val stepKey: String,
    val requiredDataForStep: List<InlineFieldSpec> = emptyList(),
)

enum class DataExchangeStatusValues {
    IN_PROGRESS, COMPLETED, FAILED
}
