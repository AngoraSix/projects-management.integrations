package com.angorasix.projects.management.integrations.application

import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAssetRepository
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSync
import org.springframework.cloud.stream.function.StreamBridge

/**
 *
 *
 * @author rozagerardo
 */
class IntegrationAssetService(
    private val repository: IntegrationAssetRepository,
    private val streamBridge: StreamBridge
) {

    /**
     * Method to modify [SourceSync].
     *
     */
//    suspend fun processAssets(
//        requestingContributor: SimpleContributor,
//        sourceSyncId: String,
//        modificationOperations: List<SourceSyncModification<out Any>>,
//    ): SourceSync? {
//
//    }
}
