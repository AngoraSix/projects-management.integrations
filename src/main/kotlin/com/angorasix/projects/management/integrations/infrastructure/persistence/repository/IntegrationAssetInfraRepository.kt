package com.angorasix.projects.management.integrations.infrastructure.persistence.repository

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAsset
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationStatusValues
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
        requestingContributor: SimpleContributor? = null,
    ): Flow<IntegrationAsset>

    suspend fun findForContributorUsingFilter(
        filter: ListIntegrationAssetFilter,
        requestingContributor: SimpleContributor,
    ): IntegrationAsset?

    suspend fun updateAllStatus(
        filter: ListIntegrationAssetFilter,
        status: IntegrationStatusValues,
    )
}
