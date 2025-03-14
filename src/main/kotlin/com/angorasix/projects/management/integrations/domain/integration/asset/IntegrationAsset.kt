package com.angorasix.projects.management.integrations.domain.integration.asset

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import java.time.Instant
import java.util.UUID

/**
 * Integration Asset Root.
 *
 * An integration asset with the third-part integration.
 *
 * @author rozagerardo
 */
data class IntegrationAsset
    @PersistenceCreator
    constructor(
        @field:Id val id: String?,
        val source: String,
        val sourceSyncId: String,
        val integrationAssetStatus: IntegrationAssetStatus,
        val sourceData: SourceAssetData,
        val sourceDto: Any,
        val angoraSixData: A6AssetData?,
    ) {
        constructor(
            source: String,
            sourceSyncId: String,
            integrationAssetStatus: IntegrationAssetStatus,
            sourceData: SourceAssetData,
            sourceDto: Any,
        ) : this(
            null,
            source,
            sourceSyncId,
            integrationAssetStatus,
            sourceData,
            sourceDto,
            null,
        )

        fun requiresUpdate(existing: IntegrationAsset): Boolean = sourceData != existing.sourceData
    }

data class IntegrationAssetStatus(
    val events: MutableList<IntegrationAssetSyncEvent> = mutableListOf(),
) {
    fun currentStatus(): IntegrationStatusValues {
        val lastEvent = events.last { it.type != IntegrationAssetSyncEventValues.POSTPONED }
        return when (lastEvent.type) {
            IntegrationAssetSyncEventValues.IMPORT -> IntegrationStatusValues.UNSYNCED
            IntegrationAssetSyncEventValues.SYNCING -> IntegrationStatusValues.SYNCING_IN_PROGRESS
            IntegrationAssetSyncEventValues.UPDATE -> IntegrationStatusValues.SYNCING_IN_PROGRESS
            IntegrationAssetSyncEventValues.ACK -> IntegrationStatusValues.SYNCED
            IntegrationAssetSyncEventValues.UNSYNC -> IntegrationStatusValues.UNSYNCED
            else -> {
                error("Invalid status based on events [$events]")
            }
        }
    }
}

data class IntegrationAssetSyncEvent(
    val type: IntegrationAssetSyncEventValues,
    val syncEventId: String = UUID.randomUUID().toString(),
    val eventInstant: Instant = Instant.now(),
) {
    constructor(type: IntegrationAssetSyncEventValues, syncEventId: String?) : this(
        type,
        syncEventId ?: UUID.randomUUID().toString(),
    )

    companion object {
        fun import(syncEventId: String? = null): IntegrationAssetSyncEvent =
            IntegrationAssetSyncEvent(IntegrationAssetSyncEventValues.IMPORT, syncEventId)

        fun syncing(syncEventId: String? = null): IntegrationAssetSyncEvent =
            IntegrationAssetSyncEvent(IntegrationAssetSyncEventValues.SYNCING, syncEventId)

        fun ack(syncEventId: String? = null): IntegrationAssetSyncEvent =
            IntegrationAssetSyncEvent(IntegrationAssetSyncEventValues.ACK, syncEventId)

        fun postponed(syncEventId: String? = null): IntegrationAssetSyncEvent =
            IntegrationAssetSyncEvent(IntegrationAssetSyncEventValues.POSTPONED, syncEventId)
    }
}

enum class IntegrationAssetSyncEventValues {
    IMPORT,
    SYNCING,
    UPDATE,
    POSTPONED,
    ACK,
    UNSYNC,
}

enum class IntegrationStatusValues {
    UNSYNCED,
    SYNCING_IN_PROGRESS,
    SYNCED,
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
    // TASK ESTIMATION (CAPS)
    val estimations: SourceAssetEstimationData? = null,
)

data class SourceAssetEstimationData(
    val caps: Double?,
    val strategy: String?,
    val effort: Double?,
    val complexity: Double?,
    val industry: String?,
    val industryModifier: Double?,
    val moneyPayment: Double?,
)

data class A6AssetData(
    val a6Id: String,
    val type: A6AssetTypeValues,
) {
    companion object {
        fun task(a6Id: String): A6AssetData = A6AssetData(a6Id, A6AssetTypeValues.TASK)
    }
}

enum class A6AssetTypeValues(
    val value: String,
) {
    TASK("task"),
}
