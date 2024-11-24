package com.angorasix.projects.management.integrations.infrastructure.persistence.repository

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSync
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.ListSourceSyncFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query

class SourceSyncInfraRepositoryImpl(private val mongoOps: ReactiveMongoOperations) :
    SourceSyncInfraRepository {

    override fun findUsingFilter(
        filter: ListSourceSyncFilter,
        requestingContributor: SimpleContributor,
    ): Flow<SourceSync> {
        return mongoOps.find(filter.toQuery(requestingContributor), SourceSync::class.java)
            .asFlow()
    }

    override suspend fun findForContributorUsingFilter(
        filter: ListSourceSyncFilter,
        requestingContributor: SimpleContributor,
    ): SourceSync? {
        return mongoOps.find(filter.toQuery(requestingContributor), SourceSync::class.java)
            .awaitFirstOrNull()
    }
}

private fun ListSourceSyncFilter.toQuery(requestingContributor: SimpleContributor): Query {
    val query = Query()

    query.addCriteria(where("admins.contributorId").`in`(requestingContributor.contributorId))

    ids?.let { query.addCriteria(where("_id").`in`(it)) }
    integrationId?.let { query.addCriteria(where("integrationId").`in`(it)) }
    sources?.let { query.addCriteria(where("source").`in`(it)) }

    return query
}