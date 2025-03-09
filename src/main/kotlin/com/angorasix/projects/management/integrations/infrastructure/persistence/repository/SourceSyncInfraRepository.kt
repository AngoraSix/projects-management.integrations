package com.angorasix.projects.management.integrations.infrastructure.persistence.repository

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSync
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.SourceSyncFilter
import kotlinx.coroutines.flow.Flow

/**
 * <p>
 * </p>
 *
 * @author rozagerardo
 */
interface SourceSyncInfraRepository {
    fun findUsingFilter(
        filter: SourceSyncFilter,
        requestingContributor: SimpleContributor? = null,
        allowAnonymous: Boolean = false,
    ): Flow<SourceSync>

    suspend fun findSingleUsingFilter(
        filter: SourceSyncFilter,
        requestingContributor: SimpleContributor? = null,
        allowAnonymous: Boolean = false,
    ): SourceSync?
}
