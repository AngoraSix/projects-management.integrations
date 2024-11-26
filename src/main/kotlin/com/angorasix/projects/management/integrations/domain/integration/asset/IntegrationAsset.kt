package com.angorasix.projects.management.integrations.domain.integration.asset

import com.angorasix.commons.domain.projectmanagement.integrations.Source
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import java.time.Instant

/**
 * Integration Asset Root.
 *
 * An integration asset with the third-part integration.
 *
 * @author rozagerardo
 */
data class IntegrationAsset @PersistenceCreator constructor(
    @field:Id val id: String?,
    val source: Source,
    val integrationId: String,
    val sourceSyncId: String,
    var integrationStatus: IntegrationStatus,
    var assetStatus: AssetStatus,
    val sourceData: SourceAssetData,
    val sourceDto: Any,
    val angoraSixData: A6AssetData?,
) {
    constructor(
        source: Source,
        integrationId: String,
        sourceSyncId: String,
        integrationStatus: IntegrationStatus,
        assetStatus: AssetStatus,
        sourceData: SourceAssetData,
        sourceDto: Any,
    ) : this(
        null,
        source,
        integrationId,
        sourceSyncId,
        integrationStatus,
        assetStatus,
        sourceData,
        sourceDto,
        null,
    )
}

data class IntegrationStatus(
    var status: IntegrationAssetStatusValues,
    val lastSyncingRequestInstant: Instant? = null,
)

enum class IntegrationAssetStatusValues {
    UNSYNCED, SYNCING_IN_PROGRESS, SYNCED
}

data class AssetStatus(
    val done: Boolean,
)

data class SourceAssetData(
    val id: String,
    val type: String,
)

data class A6AssetData(
    val id: String,
    val type: A6AssetTypeValues,
)

enum class A6AssetTypeValues(value: String) {
    TASK("task")
}
