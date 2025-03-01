package com.angorasix.projects.management.integrations.domain.integration.configuration

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSync
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * <p>
 *     Root entity defining the Project Management Integration Configuration data.
 * </p>
 *
 * @author rozagerardo
 */
@Document
@CompoundIndex(name = "integration_idx", def = "{'source': 1, 'projectManagementId': 1}")
data class Integration
    @PersistenceCreator
    public constructor(
        @field:Id val id: String?,
        val source: String,
        val projectManagementId: String, // for a particular Project Mgmt (same user/admin could link to the same source),
        val status: IntegrationStatus,
        val admins: Set<SimpleContributor> = emptySet(),
        val config: IntegrationConfig,
    ) {
        @Transient var sourceSync: SourceSync? = null

        constructor(
            source: String,
            projectManagementId: String, // for a particular Project Mgmt (same user/admin could link to the same source),
            status: IntegrationStatus,
            admins: Set<SimpleContributor> = emptySet(),
            config: IntegrationConfig,
        ) : this(
            null,
            source,
            projectManagementId,
            status,
            admins,
            config,
        )

        /**
         * Checks whether a particular contributor is Admin of this Club.
         *
         * @param contributorId - contributor candidate to check.
         */
        fun isAdmin(contributorId: String?): Boolean = (contributorId != null).and(admins.any { it.contributorId == contributorId })
    }

data class IntegrationStatus(
    @Transient var status: IntegrationStatusValues, // should match one of the IntegrationStatusValues, but flexible
    var expirationDate: Instant? = null, // the integration or syncing expiration date
    val sourceStrategyStatusData: Map<String, Any>? = null, // any information used by the source to manage its state
) {
    companion object {
        fun registered(sourceStrategyData: Map<String, Any>?): IntegrationStatus =
            IntegrationStatus(IntegrationStatusValues.REGISTERED, Instant.now(), sourceStrategyData)
    }
}

enum class IntegrationStatusValues {
    NOT_REGISTERED,
    REGISTERED,
    DISABLED,
}

data class IntegrationConfig(
    val sourceStrategyConfigData: Map<String, Any>?,
)
