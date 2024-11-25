package com.angorasix.projects.management.integrations.domain.integration.sourcesync

import com.angorasix.projects.management.integrations.infrastructure.persistence.repository.SourceSyncInfraRepository
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.kotlin.CoroutineSortingRepository

/**
 *
 * Integration Source Sync Repository.
 *
 * @author rozagerardo
 */
interface SourceSyncRepository :
    CoroutineCrudRepository<SourceSync, String>,
    CoroutineSortingRepository<SourceSync, String>,
    SourceSyncInfraRepository
