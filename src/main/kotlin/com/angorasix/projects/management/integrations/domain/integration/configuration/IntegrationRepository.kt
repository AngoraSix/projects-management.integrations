package com.angorasix.projects.management.integrations.domain.integration.configuration

import com.angorasix.projects.management.integrations.infrastructure.persistence.repository.IntegrationInfraRepository
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.kotlin.CoroutineSortingRepository

/**
 *
 * Project Management Integration Repository.
 *
 * @author rozagerardo
 */
interface IntegrationRepository :
    CoroutineCrudRepository<Integration, String>,
    CoroutineSortingRepository<Integration, String>,
    IntegrationInfraRepository {
    suspend fun findBySourceAndProjectManagementId(source: String, projectManagementId: String): Integration?
}
