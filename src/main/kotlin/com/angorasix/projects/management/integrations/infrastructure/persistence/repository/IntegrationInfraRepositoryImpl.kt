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
class IntegrationInfraRepositoryImpl(
    private val mongoOps: ReactiveMongoOperations,
) : IntegrationInfraRepository {
    override fun findUsingFilter(
        filter: ListIntegrationFilter,
        requestingContributor: SimpleContributor?,
        allowAnonymous: Boolean,
    ): Flow<Integration> =
        mongoOps
            .find(filter.toQuery(requestingContributor, allowAnonymous), Integration::class.java)
            .asFlow()

    override suspend fun findSingleUsingFilter(
        filter: ListIntegrationFilter,
        requestingContributor: SimpleContributor?,
        allowAnonymous: Boolean,
    ): Integration? =
        mongoOps
            .find(filter.toQuery(requestingContributor, allowAnonymous), Integration::class.java)
            .awaitFirstOrNull()
}

private fun ListIntegrationFilter.toQuery(
    requestingContributor: SimpleContributor?,
    allowAnonymous: Boolean = false,
): Query {
    if (!allowAnonymous) {
        requireNotNull(requestingContributor)
    }
    val query = Query()

    requestingContributor?.let { query.addCriteria(where("admins.contributorId").`is`(it.contributorId)) }

    ids?.let { query.addCriteria(where("_id").`in`(it as Collection<Any>)) }
    projectManagementId?.let {
        query.addCriteria(where("projectManagementId").`in`(it as Collection<Any>))
    }
    sources?.let { query.addCriteria(where("source").`in`(it as Collection<Any>)) }

    return query
}
