package com.angorasix.projects.management.integrations.infrastructure.persistence.repository

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.projects.management.integrations.domain.integration.exchange.DataExchange
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.ListDataExchangeFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query

class DataExchangeInfraRepositoryImpl(private val mongoOps: ReactiveMongoOperations) :
    DataExchangeInfraRepository {

    override fun findUsingFilter(
        filter: ListDataExchangeFilter,
        requestingContributor: SimpleContributor,
    ): Flow<DataExchange> {
        return mongoOps.find(filter.toQuery(requestingContributor), DataExchange::class.java)
            .asFlow()
    }

    override suspend fun findForContributorUsingFilter(
        filter: ListDataExchangeFilter,
        requestingContributor: SimpleContributor,
    ): DataExchange? {
        return mongoOps.find(filter.toQuery(requestingContributor), DataExchange::class.java)
            .awaitFirstOrNull()
    }
}

private fun ListDataExchangeFilter.toQuery(requestingContributor: SimpleContributor): Query {
    val query = Query()

    query.addCriteria(where("admins.contributorId").`in`(requestingContributor.contributorId))

    ids?.let { query.addCriteria(where("_id").`in`(it)) }
    integrationId?.let { query.addCriteria(where("integrationId").`in`(it)) }
    sources?.let { query.addCriteria(where("source").`in`(it)) }

    return query
}
