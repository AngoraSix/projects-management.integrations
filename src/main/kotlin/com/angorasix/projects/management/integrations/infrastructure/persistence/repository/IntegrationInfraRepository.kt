package com.angorasix.projects.management.integrations.infrastructure.persistence.repository

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAsset
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
    fun findUsingFilter(filter: ListIntegrationFilter): Flow<Integration>
//    suspend fun findByIdForContributor(
//        filter: ListIntegrationFilter,
//        requestingContributor: SimpleContributor?,
//    ): Integration?

//    suspend fun updateOrCreate(integrations: List<IntegrationAsset>): BulkResult
}
