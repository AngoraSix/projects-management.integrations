package com.angorasix.projects.management.integrations.infrastructure.persistence.repository

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.projects.management.integrations.domain.integration.configuration.Integration
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.ListIntegrationFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query

/**
 * <p>
 * </p>
 *
 * @author rozagerardo
 */
class IntegrationInfraRepositoryImpl(private val mongoOps: ReactiveMongoOperations) :
    IntegrationInfraRepository {

    override fun findUsingFilter(
        filter: ListIntegrationFilter,
        requestingContributor: SimpleContributor,
    ): Flow<Integration> {
        return mongoOps.find(filter.toQuery(requestingContributor), Integration::class.java)
            .asFlow()
    }

    override suspend fun findSingleForContributorUsingFilter(
        filter: ListIntegrationFilter,
        requestingContributor: SimpleContributor,
    ): Integration? {
        return mongoOps.find(filter.toQuery(requestingContributor), Integration::class.java)
            .awaitFirstOrNull()
    }
}

private fun ListIntegrationFilter.toQuery(requestingContributor: SimpleContributor): Query {
    val query = Query()

    query.addCriteria(where("admins.contributorId").`in`(requestingContributor.contributorId))

    ids?.let { query.addCriteria(where("_id").`in`(it)) }
    projectManagementId?.let { query.addCriteria(where("projectManagementId").`in`(it)) }
    sources?.let { query.addCriteria(where("source").`in`(it)) }

    return query
}
