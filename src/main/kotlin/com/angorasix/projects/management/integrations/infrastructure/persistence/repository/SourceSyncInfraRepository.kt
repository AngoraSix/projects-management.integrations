package com.angorasix.projects.management.integrations.infrastructure.persistence.repository

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.projects.management.integrations.domain.integration.configuration.Integration
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSync
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.ListIntegrationFilter
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.ListSourceSyncFilter
import kotlinx.coroutines.flow.Flow

/**
 * <p>
 * </p>
 *
 * @author rozagerardo
 */
interface SourceSyncInfraRepository {
    fun findUsingFilter(
        filter: ListSourceSyncFilter,
        requestingContributor: SimpleContributor? = null,
    ): Flow<SourceSync>

    suspend fun findForContributorUsingFilter(
        filter: ListSourceSyncFilter,
        requestingContributor: SimpleContributor,
    ): SourceSync?
}
