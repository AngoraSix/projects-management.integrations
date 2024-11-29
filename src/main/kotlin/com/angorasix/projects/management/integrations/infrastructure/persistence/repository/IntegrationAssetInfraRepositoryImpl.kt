package com.angorasix.projects.management.integrations.infrastructure.persistence.repository

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAsset
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationStatus
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationStatusValues
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.ListIntegrationAssetFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import java.time.Instant

class IntegrationAssetInfraRepositoryImpl(private val mongoOps: ReactiveMongoOperations) :
    IntegrationAssetInfraRepository {

    override fun findUsingFilter(
        filter: ListIntegrationAssetFilter,
        requestingContributor: SimpleContributor?,
    ): Flow<IntegrationAsset> {
        return mongoOps.find(filter.toQuery(requestingContributor), IntegrationAsset::class.java)
            .asFlow()
    }

    override suspend fun findForContributorUsingFilter(
        filter: ListIntegrationAssetFilter,
        requestingContributor: SimpleContributor,
    ): IntegrationAsset? {
        return mongoOps.find(filter.toQuery(requestingContributor), IntegrationAsset::class.java)
            .awaitFirstOrNull()
    }

    override suspend fun updateAllStatus(
        filter: ListIntegrationAssetFilter,
        status: IntegrationStatusValues,
    ) {
        mongoOps.updateMulti(
            filter.toAllByIdQuery(),
            statusUpdate(status),
            IntegrationAsset::class.java,
        ).awaitFirstOrNull()
    }
}

private fun ListIntegrationAssetFilter.toQuery(requestingContributor: SimpleContributor?): Query {
    val query = Query()

    requestingContributor?.let {
        query.addCriteria(
            where("admins.contributorId").`in`(
                requestingContributor.contributorId,
            ),
        )
    }

    ids?.let { query.addCriteria(where("_id").`in`(it)) }
    assetDataId?.let { query.addCriteria(where("sourceData.id").`in`(it)) }
    sourceSyncId?.let { query.addCriteria(where("sourceSyncId").`in`(it)) }
    integrationId?.let { query.addCriteria(where("integrationId").`in`(it)) }
    sources?.let { query.addCriteria(where("source").`in`(it)) }

    return query
}

private fun ListIntegrationAssetFilter.toAllByIdQuery(): Query {
    val query = Query()
    ids?.let { query.addCriteria(where("_id").`in`(it)) }
    return query
}

private fun statusUpdate(statusValue: IntegrationStatusValues): Update {
    return Update().set("status", IntegrationStatus(statusValue, Instant.now()))
}