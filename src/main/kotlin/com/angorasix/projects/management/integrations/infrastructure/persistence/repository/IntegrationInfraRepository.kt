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
        requestingContributor: SimpleContributor? = null,
        allowAnonymous: Boolean = false,
    ): Flow<Integration>

    suspend fun findSingleUsingFilter(
        filter: ListIntegrationFilter,
        requestingContributor: SimpleContributor? = null,
        allowAnonymous: Boolean = false,
    ): Integration?
}
