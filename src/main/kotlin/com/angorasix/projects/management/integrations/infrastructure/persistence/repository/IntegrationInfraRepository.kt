package com.angorasix.projects.management.integrations.infrastructure.persistence.repository

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.projects.management.integrations.domain.integration.configuration.Integration
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.ListIntegrationFilter
import kotlinx.coroutines.flow.Flow

/**
 * <p>
 * </p>
 *
 * @author rozagerardo
 */
interface IntegrationInfraRepository {
    fun findUsingFilter(
        filter: ListIntegrationFilter,
        requestingContributor: SimpleContributor,
    ): Flow<Integration>

    suspend fun findSingleForContributorUsingFilter(
        filter: ListIntegrationFilter,
        requestingContributor: SimpleContributor,
    ): Integration?

//    suspend fun updateOrCreate(integrations: List<IntegrationAsset>): BulkResult
}
