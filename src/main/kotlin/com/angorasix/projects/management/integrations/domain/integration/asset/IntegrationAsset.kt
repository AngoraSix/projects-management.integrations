package com.angorasix.projects.management.integrations.domain.integration.asset

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
    val source: String,
    val integrationId: String,
    val sourceSyncId: String,
    val integrationStatus: IntegrationStatus,
    val sourceData: SourceAssetData,
    val sourceDto: Any,
    val angoraSixData: A6AssetData?,
) {
    constructor(
        source: String,
        integrationId: String,
        sourceSyncId: String,
        integrationStatus: IntegrationStatus,
        sourceData: SourceAssetData,
        sourceDto: Any,
    ) : this(
        null,
        source,
        integrationId,
        sourceSyncId,
        integrationStatus,
        sourceData,
        sourceDto,
        null,
    )

    fun requiresUpdate(existing: IntegrationAsset): Boolean {
        return sourceData != existing.sourceData
    }
}

data class IntegrationStatus(
    val status: IntegrationStatusValues,
    val lastSyncingRequestInstant: Instant? = null,
)

enum class IntegrationStatusValues {
    UNSYNCED, SYNCING_IN_PROGRESS, SYNCED
}

data class SourceAssetData(
    val id: String,
    val type: String,
    // TASK
    val title: String,
    val description: String?,
    val dueInstant: Instant?,
    val assigneeIds: List<String> = emptyList(),
    val done: Boolean = false,
)

data class A6AssetData(
    val id: String,
    val type: A6AssetTypeValues,
)

enum class A6AssetTypeValues(value: String) {
    TASK("task")
}
