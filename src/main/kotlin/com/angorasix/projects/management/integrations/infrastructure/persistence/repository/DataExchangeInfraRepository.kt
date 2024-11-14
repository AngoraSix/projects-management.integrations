package com.angorasix.projects.management.integrations.infrastructure.persistence.repository

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.projects.management.integrations.domain.integration.exchange.DataExchange
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.ListDataExchangeFilter
import kotlinx.coroutines.flow.Flow

/**
 * <p>
 * </p>
 *
 * @author rozagerardo
 */
interface DataExchangeInfraRepository {
    fun findUsingFilter(
        filter: ListDataExchangeFilter,
        requestingContributor: SimpleContributor,
    ): Flow<DataExchange>

    suspend fun findForContributorUsingFilter(
        filter: ListDataExchangeFilter,
        requestingContributor: SimpleContributor,
    ): DataExchange?
}
