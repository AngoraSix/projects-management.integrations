package com.angorasix.projects.management.integrations.presentation.dto

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.presentation.dto.InlineFieldSpecDto
import com.angorasix.commons.presentation.dto.PatchOperation
import com.angorasix.commons.presentation.dto.PatchOperationSpec
import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationStatusValues
import com.angorasix.projects.management.integrations.domain.integration.configuration.modification.IntegrationModification
import com.angorasix.projects.management.integrations.domain.integration.configuration.modification.ModifyIntegrationStatus
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncEvent
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncEventValues
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncStatusValues
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.modification.ReplaceStepResponseData
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.modification.RequestFullSyncEvent
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.modification.RequestSyncConfigUpdate
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.modification.SourceSyncModification
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.time.Instant

/**
 *
 *
 * @author rozagerardo
 */
@Relation(collectionRelation = "integrationList", itemRelation = "integration")
data class IntegrationDto(
    val source: String? = null,
    val projectManagementId: String? = null,
    val status: IntegrationStatusDto? = null,
    val admins: Set<SimpleContributor>? = emptySet(),
    val config: IntegrationConfigDto? = null,
    val sourceSync: SourceSyncDto? = null,
    val id: String? = null,
) : RepresentationModel<IntegrationDto>()

data class IntegrationStatusDto(
    val status: IntegrationStatusValues? = null,
    val expirationDate: Instant? = null,
    val sourceStrategyData: Map<String, Any>? = null,
)

data class IntegrationConfigDto(val sourceStrategyConfigData: Map<String, Any>?)

enum class SupportedIntegrationPatchOperations(val op: PatchOperationSpec) {
    STATUS(
        object : PatchOperationSpec {
            override fun supportsPatchOperation(operation: PatchOperation): Boolean =
                operation.op == "replace" && operation.path == "/status/status"

            override fun mapToObjectModification(
                contributor: SimpleContributor,
                operation: PatchOperation,
                objectMapper: ObjectMapper,
            ): IntegrationModification<IntegrationStatusValues> {
                val statusValue =
                    objectMapper.treeToValue(operation.value, IntegrationStatusValues::class.java)
                        ?: throw IllegalArgumentException(
                            "Not supported value: ${operation.value}." +
                                "Supported values: [${IntegrationStatusValues.values()}]",
                        )
                return ModifyIntegrationStatus(statusValue)
            }
        },
    ),
}

data class SourceSyncDto(
    val source: String? = null,
    val integrationId: String? = null,
    val status: SourceSyncStatusDto? = null,
    val events: List<SourceSyncEventDto> = listOf(),
    val sourceStrategyStateData: Any? = null,
    val id: String? = null,
    val sourceAssets: List<IntegrationAssetDto> = emptyList(),
) : RepresentationModel<SourceSyncDto>()

data class IntegrationAssetDto(
    val id: String? = null,
    val integrationStatus: IntegrationAssetStatusDto,
    val sourceData: SourceAssetDataDto,

    val source: String,
    val integrationId: String,
    val sourceSyncId: String,
)

data class SourceAssetDataDto(
    val id: String,
    val type: String,
    // TASK
    val title: String,
    val description: String?,
    val dueInstant: Instant?,
    val assigneeIds: List<String> = emptyList(),
    val done: Boolean = false,
    // TASK ESTIMATION (CAPS)
    val estimations: SourceAssetEstimationDataDto? = null,
)

data class SourceAssetEstimationDataDto(
    val caps: Double?,
    val strategy: String?,
    val effort: Double?,
    val complexity: Double?,
    val industry: String?,
    val industryModifier: Double?,
    val moneyPayment: Double?,
)

data class IntegrationAssetStatusDto(
    val events: List<IntegrationAssetSyncEventDto> = emptyList(),
)

data class IntegrationAssetSyncEventDto(
    val type: String,
    val syncEventId: String,
    val eventInstant: String,
)

data class SourceSyncEventDto(
    val type: SourceSyncEventValues,
    val eventInstant: Instant? = null,
)

data class SourceSyncStatusDto(
    val status: SourceSyncStatusValues,
    val steps: List<SourceSyncStatusStepDto> = emptyList(),
)

data class SourceSyncStatusStepDto(
    val stepKey: String,
    val requiredDataForStep: List<InlineFieldSpecDto> = emptyList(),
    var responseData: Map<String, List<String>>? = null,
)

enum class SupportedSourceSyncPatchOperations(val op: PatchOperationSpec) {
    STEP_RESPONSE_DATA(
        object : PatchOperationSpec {
            override fun supportsPatchOperation(operation: PatchOperation): Boolean =
                operation.op == "replace" && Regex("^/status/steps/\\d+$").matches(operation.path)

            override fun mapToObjectModification(
                contributor: SimpleContributor,
                operation: PatchOperation,
                objectMapper: ObjectMapper,
            ): SourceSyncModification<List<Map<String, List<String>>?>> {
                val responseDataValue =
                    objectMapper.convertValue(
                        operation.value,
                        object : TypeReference<Map<String, List<String>>>() {},
                    )
                        ?: throw IllegalArgumentException(
                            "Not supported value: ${operation.value}." +
                                "Supported values: [${IntegrationStatusValues.values()}]",
                        )
                val index = extractNumberFromPath(operation.path)
                val indexedResponse = MutableList<Map<String, List<String>>?>(index + 1) { null }
                indexedResponse.add(index, responseDataValue)
                return ReplaceStepResponseData(indexedResponse)
            }
        },
    ),
    REQUEST_FULL_SYNC_EVENT(
        object : PatchOperationSpec {
            override fun supportsPatchOperation(operation: PatchOperation): Boolean =
                operation.op == "add" && operation.path == "/events/+"

            override fun mapToObjectModification(
                contributor: SimpleContributor,
                operation: PatchOperation,
                objectMapper: ObjectMapper,
            ): SourceSyncModification<SourceSyncEvent> {
                val eventValue =
                    objectMapper.treeToValue(operation.value, SourceSyncEventDto::class.java)

                return RequestFullSyncEvent(
                    SourceSyncEvent(
                        eventValue.type,
                    ),
                )
            }
        },
    ),
    REQUEST_SYNC_CONFIG_UPDATE(
        object : PatchOperationSpec {
            override fun supportsPatchOperation(operation: PatchOperation): Boolean =
                operation.op == "replace" && operation.path == "/status/status"

            override fun mapToObjectModification(
                contributor: SimpleContributor,
                operation: PatchOperation,
                objectMapper: ObjectMapper,
            ): SourceSyncModification<SourceSyncStatusValues> {
                val statusValue =
                    objectMapper.treeToValue(operation.value, SourceSyncStatusValues::class.java)
                return RequestSyncConfigUpdate(statusValue)
            }
        },
    ),
}

fun extractNumberFromPath(path: String): Int {
    val regex = Regex("^/status/steps/(\\d+)$")
    val matchResult = regex.matchEntire(path)
    return matchResult?.groups?.get(1)?.value?.toInt()
        ?: throw IllegalArgumentException("Invalid path")
}
