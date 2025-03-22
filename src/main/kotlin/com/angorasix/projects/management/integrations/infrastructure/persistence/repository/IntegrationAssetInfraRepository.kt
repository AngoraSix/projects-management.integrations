package com.angorasix.projects.management.integrations.infrastructure.persistence.repository

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAsset
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAssetSyncEvent
import com.angorasix.projects.management.integrations.infrastructure.domain.SourceSyncContext
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.ListIntegrationAssetFilter
import kotlinx.coroutines.flow.Flow

/**
 * <p>
 * </p>
 *
 * @author rozagerardo
 */
interface IntegrationAssetInfraRepository {
    fun findUsingFilter(
        filter: ListIntegrationAssetFilter,
        sourceSyncContext: SourceSyncContext,
        requestingContributor: SimpleContributor? = null,
        allowAnonymous: Boolean = false,
    ): Flow<IntegrationAsset>

    suspend fun findSingleUsingFilter(
        filter: ListIntegrationAssetFilter,
        sourceSyncContext: SourceSyncContext,
        requestingContributor: SimpleContributor? = null,
        allowAnonymous: Boolean = false,
    ): IntegrationAsset?

    suspend fun registerEvent(
        filter: ListIntegrationAssetFilter,
        event: IntegrationAssetSyncEvent,
        sourceSyncContext: SourceSyncContext,
        requestingContributor: SimpleContributor? = null,
        allowAnonymous: Boolean = false,
    )

    suspend fun registerCorrespondences(
        correspondences: List<Pair<String, String>>,
        syncingEventId: String,
        sourceSyncContext: SourceSyncContext,
        requestingContributor: SimpleContributor? = null,
        allowAnonymous: Boolean = false,
    )
}
