package com.angorasix.projects.management.integrations.presentation.dto

import com.angorasix.commons.domain.DetailedContributor
import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.presentation.dto.InlineFieldSpecDto
import com.angorasix.commons.presentation.dto.PatchOperation
import com.angorasix.commons.presentation.dto.PatchOperationSpec
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncEvent
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncEventValues
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncStatusValues
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.modification.ModifySourceSyncStatus
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.modification.ReplaceMappingUsersData
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.modification.ReplaceStepResponseData
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.modification.RequestFullSyncEvent
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.modification.SourceSyncModification
import com.fasterxml.jackson.annotation.JsonProperty
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
@Relation(collectionRelation = "sourceSyncList", itemRelation = "sourceSync")
data class SourceSyncDto(
    val source: String? = null,
    val projectManagementId: String? = null,
    val status: SourceSyncStatusDto? = null,
    val config: SourceSyncConfigDto? = null,
    val id: String? = null,
) : RepresentationModel<SourceSyncDto>()

data class SourceSyncStatusDto(
    val status: SourceSyncStatusValues? = null,
    val expirationDate: Instant? = null,
)

data class SourceSyncConfigDto(
    val steps: List<SourceSyncStatusStepDto> = emptyList(),
    // accessToken just for request inputs, don't retrieve it even if it's encrypted
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    val accessToken: String? = null,
)

data class SourceSyncEventDto(
    val type: SourceSyncEventValues,
    val eventInstant: Instant? = null,
)

data class SourceSyncStatusStepDto(
    val stepKey: String,
    val requiredDataForStep: List<InlineFieldSpecDto> = emptyList(),
    var responseData: Map<String, List<String>>? = null,
)

data class ProjectContributorsToMatchDto(
    val projectContributors: List<DetailedContributor>,
)

enum class SupportedSourceSyncPatchOperations(
    val op: PatchOperationSpec,
) {
    UPDATE_STATUS(
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
                        ?: throw IllegalArgumentException(
                            "Not supported value: ${operation.value}." +
                                "Supported values: [${SourceSyncStatusValues.values()}]",
                        )
                return ModifySourceSyncStatus(statusValue)
            }
        },
    ),
    STEP_RESPONSE_DATA(
        object : PatchOperationSpec {
            override fun supportsPatchOperation(operation: PatchOperation): Boolean =
                operation.op == "replace" && Regex("^/config/steps/\\d+$").matches(operation.path)

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
                                "Supported values: [${SourceSyncStatusValues.values()}]",
                        )
                val index = extractNumberFromPath(operation.path)
                val indexedResponse = MutableList<Map<String, List<String>>?>(index + 1) { null }
                indexedResponse.add(index, responseDataValue)
                return ReplaceStepResponseData(indexedResponse)
            }
        },
    ),
    MAPPINGS_UPDATE_DATA(
        object : PatchOperationSpec {
            override fun supportsPatchOperation(operation: PatchOperation): Boolean =
                operation.op == "replace" && operation.path == "/mappings/users"

            override fun mapToObjectModification(
                contributor: SimpleContributor,
                operation: PatchOperation,
                objectMapper: ObjectMapper,
            ): SourceSyncModification<Map<String, String>> {
                val mappingValueData =
                    objectMapper.convertValue(
                        operation.value,
                        object : TypeReference<Map<String, String>>() {},
                    )
                        ?: throw IllegalArgumentException(
                            "Not supported value: ${operation.value}.",
                        )
                return ReplaceMappingUsersData(mappingValueData)
            }
        },
    ),
    REQUEST_FULL_SYNC_EVENT(
        object : PatchOperationSpec {
            override fun supportsPatchOperation(operation: PatchOperation): Boolean = operation.op == "add" && operation.path == "/events/+"

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
}

data class IntegrationAssetDto(
    val id: String? = null,
    val integrationStatus: IntegrationAssetStatusDto,
    val sourceData: SourceAssetDataDto,
    val source: String,
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

class SourceSyncMappingsUsersInputCollectionModel(
    val source: String,
    usersInputs: List<InlineFieldSpecDto>,
) : RepresentationModel<SourceSyncMappingsUsersInputCollectionModel>() {
    @JsonProperty("_embedded")
    val embedded: SourceSyncMappingsUsersInputItem = SourceSyncMappingsUsersInputItem(usersInputs)
}

data class SourceSyncMappingsUsersInputItem(
    val usersInputs: List<InlineFieldSpecDto>,
)

fun extractNumberFromPath(path: String): Int {
    val regex = Regex("^/config/steps/(\\d+)$")
    val matchResult = regex.matchEntire(path)
    return matchResult
        ?.groups
        ?.get(1)
        ?.value
        ?.toInt()
        ?: throw IllegalArgumentException("Invalid path")
}
