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
        return mongoOps.find(filter.toQuery(requestingContributor), Integration::class.java).asFlow()
    }

    override suspend fun findByIdForContributor(
        id: String,
        requestingContributor: SimpleContributor,
    ): Integration? {
        val filter = ListIntegrationFilter()
        return mongoOps.find(filter.toQuery(requestingContributor), Integration::class.java)
            .awaitFirstOrNull()
    }

//    override suspend fun updateOrCreate(integrations: List<IntegrationAsset>): BulkResult {
//        val bulkOps = mongoOps.bulkOps(
//            BulkOperations.BulkMode.UNORDERED, IntegrationAsset::class.java,
//        )
//        integrations.forEach {
//            if (it.id != null) {
//                bulkOps.updateOne(Query(where("id").`is`(it.id)), updateDefinition(it)) //, Integration::class.java)
//            } else {
//                bulkOps.insert(it)
//            }
//        }
//        return bulkOps.execute().awaitFirst().toDto()
//    }
}

// private fun updateDefinition(integration: IntegrationAsset): UpdateDefinition =
//    Update().set("title", integration.title).set("description", integration.description)
//    .set("estimation", integration.estimation)
//        .set("assignees", integration.assignees)

private fun ListIntegrationFilter.toQuery(requestingContributor: SimpleContributor): Query {
    val query = Query()

    query.addCriteria(where("admins.contributorId").`in`(requestingContributor.contributorId))

    ids?.let { query.addCriteria(where("_id").`in`(it)) }
    projectManagementId?.let { query.addCriteria(where("projectManagementId").`in`(it)) }
    sources?.let { query.addCriteria(where("source").`in`(it)) }

    if (adminId != null) {
        query.addCriteria(where("admins.contributorId").`in`(adminId + requestingContributor.contributorId))
    }

    return query
}

// data class BulkResult(
//    val inserted: Int,
//    val modified: Int,
// )

// private fun BulkWriteResult.toDto() = BulkResult(insertedCount, modifiedCount)
