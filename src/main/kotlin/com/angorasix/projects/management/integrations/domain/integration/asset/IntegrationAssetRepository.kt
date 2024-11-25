package com.angorasix.projects.management.integrations.domain.integration.asset

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.kotlin.CoroutineSortingRepository

/**
 *
 * Integration Source Sync Repository.
 *
 * @author rozagerardo
 */
interface IntegrationAssetRepository :
    CoroutineCrudRepository<IntegrationAsset, String>,
    CoroutineSortingRepository<IntegrationAsset, String>
