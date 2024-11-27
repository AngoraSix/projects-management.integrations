package com.angorasix.projects.management.integrations.application

import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAsset
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAssetRepository
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationStatus
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationStatusValues
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSync
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.ListIntegrationAssetFilter
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
        sourceSyncId: String,
    ): List<IntegrationAsset> {
        val existingSourceSyncAssets = repository.findUsingFilter(
            ListIntegrationAssetFilter(
                null,
                assets.map { it.sourceData.id },
                listOf(sourceSyncId),
            ),
        ).toList()
        val updatedAssets = assets.mapNotNull {
            updatedAssetOrNull(
                it,
                existingSourceSyncAssets.find { existing -> existing.sourceData.id == it.sourceData.id },
            )
        }
        val persistedAssets = repository.saveAll(updatedAssets).toList()
        println("ACA PASA ALGO QUE PUEDE FALLAR")
//        streamBridge.send()
        return repository.saveAll(
            persistedAssets.map {
                it.copy(
                    integrationStatus = IntegrationStatus(
                        IntegrationStatusValues.SYNCING_IN_PROGRESS,
                        Instant.now(),
                    ),
                )
            },
        ).toList()
    }
}

private fun updatedAssetOrNull(
    asset: IntegrationAsset,
    existing: IntegrationAsset?,
): IntegrationAsset? {
    return if (existing != null) {
        if (asset.requiresUpdate(existing)) {
            asset.copy(id = existing.id)
        } else {
            null
        }
    } else {
        asset
    }
}
