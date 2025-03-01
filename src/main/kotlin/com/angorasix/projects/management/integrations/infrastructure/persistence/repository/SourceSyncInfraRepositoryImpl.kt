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

class SourceSyncInfraRepositoryImpl(
    private val mongoOps: ReactiveMongoOperations,
) : SourceSyncInfraRepository {
    override fun findUsingFilter(
        filter: ListSourceSyncFilter,
        requestingContributor: SimpleContributor?,
        allowAnonymous: Boolean,
    ): Flow<SourceSync> =
        mongoOps
            .find(filter.toQuery(requestingContributor, allowAnonymous), SourceSync::class.java)
            .asFlow()

    override suspend fun findSingleUsingFilter(
        filter: ListSourceSyncFilter,
        requestingContributor: SimpleContributor?,
        allowAnonymous: Boolean,
    ): SourceSync? =
        mongoOps
            .find(filter.toQuery(requestingContributor, allowAnonymous), SourceSync::class.java)
            .awaitFirstOrNull()
}

private fun ListSourceSyncFilter.toQuery(
    requestingContributor: SimpleContributor?,
    allowAnonymous: Boolean = false,
): Query {
    if (!allowAnonymous) {
        requireNotNull(requestingContributor)
    }
    val query = Query()

    requestingContributor?.let {
        query.addCriteria(where("admins.contributorId").`is`(it.contributorId))
    }

    ids?.let { query.addCriteria(where("_id").`in`(it as Collection<Any>)) }
    integrationId?.let { query.addCriteria(where("integrationId").`in`(it as Collection<Any>)) }
    sources?.let { query.addCriteria(where("source").`in`(it as Collection<Any>)) }

    return query
}
