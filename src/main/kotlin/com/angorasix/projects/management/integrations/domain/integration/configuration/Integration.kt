package com.angorasix.projects.management.integrations.domain.integration.configuration

import com.angorasix.commons.domain.SimpleContributor
import jakarta.validation.constraints.NotNull
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
class Integration @PersistenceCreator public constructor(
    @field:Id val id: String?,
    val source: String,
    val projectManagementId: String, // for a particular Project Management (same user/admin could link to the same source),
    val status: IntegrationStatus,
    val admins: Set<SimpleContributor> = emptySet(),
    val config: IntegrationConfig,
) {
    constructor(
        source: String,
        projectManagementId: String, // for a particular Project Management (same user/admin could link to the same source),
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
}

data class IntegrationStatus(
    @Transient val status: IntegrationStatusValues, // should match one of the IntegrationStatusValues, but flexible
    val expirationDate: Instant? = null, // if the integration or syncing as an expiration date
    val sourceStrategyData: Any? = null, // any information used by the integration/source strategy to manage its state
)

enum class IntegrationStatusValues {
    NOT_REGISTERED, SYNCED, UNSYNCED
}

data class IntegrationConfig(
    val sourceStrategyConfigData: Any?, // any information used by the integration/source strategy to retrieve data
)