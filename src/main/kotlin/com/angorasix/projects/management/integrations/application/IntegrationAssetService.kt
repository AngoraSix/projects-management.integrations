package com.angorasix.projects.management.integrations.application

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAssetStatusValues
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAsset
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAssetRepository
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationStatus
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSync
import kotlinx.coroutines.flow.toList
import org.springframework.cloud.stream.function.StreamBridge
import java.time.Instant

/**
 *
 *
 * @author rozagerardo
 */
class IntegrationAssetService(
    private val repository: IntegrationAssetRepository,
    private val streamBridge: StreamBridge,
) {

    /**
     * Method to modify [SourceSync].
     *
     */
    suspend fun processAssets(
        assets: List<IntegrationAsset>,
        requestingContributor: SimpleContributor,
    ): List<IntegrationAsset> {
        val persistedAssets = repository.saveAll(assets).toList()
        println("ACA PASA ALGO QUE PUEDE FALLAR")
//        streamBridge.send()
        return repository.saveAll(
            persistedAssets.map {
                it.integrationStatus =
                    IntegrationStatus(IntegrationAssetStatusValues.SYNCING_IN_PROGRESS, Instant.now())
                it
            },
        ).toList()
    }
}
