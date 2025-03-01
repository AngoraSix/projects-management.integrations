package com.angorasix.projects.management.integrations.infrastructure.persistence.repository

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.projects.management.integrations.domain.integration.asset.A6AssetData
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAsset
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAssetSyncEvent
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.ListIntegrationAssetFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.BulkOperations
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Criteria.where
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

class IntegrationAssetInfraRepositoryImpl(
    private val mongoOps: ReactiveMongoOperations,
) : IntegrationAssetInfraRepository {
    override fun findUsingFilter(
        filter: ListIntegrationAssetFilter,
        requestingContributor: SimpleContributor?,
        allowAnonymous: Boolean,
    ): Flow<IntegrationAsset> =
        mongoOps
            .find(filter.toQuery(requestingContributor, allowAnonymous), IntegrationAsset::class.java)
            .asFlow()

    override suspend fun findSingleUsingFilter(
        filter: ListIntegrationAssetFilter,
        requestingContributor: SimpleContributor?,
        allowAnonymous: Boolean,
    ): IntegrationAsset? =
        mongoOps
            .find(filter.toQuery(requestingContributor), IntegrationAsset::class.java)
            .awaitFirstOrNull()

    override suspend fun registerEvent(
        filter: ListIntegrationAssetFilter,
        event: IntegrationAssetSyncEvent,
    ) {
        mongoOps
            .updateMulti(
                filter.toAllByIdQuery(),
                addEvent(event),
                IntegrationAsset::class.java,
            ).awaitFirstOrNull()
    }

    override suspend fun registerCorrespondences(
        correspondences: List<Pair<String, String>>,
        sourceSyncId: String,
        syncingEventId: String,
    ) {
        val bulkOps =
            mongoOps.bulkOps(BulkOperations.BulkMode.UNORDERED, IntegrationAsset::class.java)
        val ackEvent = IntegrationAssetSyncEvent.ack(syncingEventId)

        correspondences.forEach {
            val query = correspondenceQuery(it.first, sourceSyncId)
            val update =
                Update()
                    .push("integrationStatus.events", ackEvent)
                    .set("angoraSixData", A6AssetData.task(it.second))
            bulkOps.updateOne(query, update)
        }
        bulkOps.execute().awaitFirstOrNull()
    }
}

private fun correspondenceQuery(
    id: String,
    sourceSyncId: String,
): Query {
    val query = Query()
    query.addCriteria(where("_id").`is`(id))
    query.addCriteria(where("sourceSyncId").`is`(sourceSyncId))
    return query
}

private fun ListIntegrationAssetFilter.toQuery(
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
    assetDataId?.let { query.addCriteria(where("sourceData.id").`in`(it as Collection<Any>)) }
    sourceSyncId?.let { query.addCriteria(where("sourceSyncId").`in`(it as Collection<Any>)) }
    integrationId?.let { query.addCriteria(where("integrationId").`in`(it as Collection<Any>)) }
    sources?.let { query.addCriteria(where("source").`in`(it as Collection<Any>)) }

    return query
}

private fun ListIntegrationAssetFilter.toAllByIdQuery(): Query {
    val query = Query()
    ids?.let { query.addCriteria(where("_id").`in`(it as Collection<Any>)) }
    return query
}

private fun addEvent(event: IntegrationAssetSyncEvent): Update = Update().push("integrationStatus.events", event)
