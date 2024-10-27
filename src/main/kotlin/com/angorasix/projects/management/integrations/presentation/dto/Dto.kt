package com.angorasix.projects.management.integrations.presentation.dto

import IntegrationModification
import ModifyIntegrationStatus
import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.presentation.dto.PatchOperation
import com.angorasix.commons.presentation.dto.PatchOperationSpec
import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationStatusValues
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
    val id: String? = null,
) : RepresentationModel<IntegrationDto>()

data class IntegrationStatusDto(
    val status: IntegrationStatusValues? = null,
    val expirationDate: Instant? = null,
    val sourceStrategyData: Map<String, Any>? = null,
)

data class IntegrationConfigDto(val sourceStrategyConfigData: Map<String, Any>?)

enum class SupportedPatchOperations(val op: PatchOperationSpec) {
    STATUS(
        object : PatchOperationSpec {
            override fun supportsPatchOperation(operation: PatchOperation): Boolean =
                operation.op == "replace" && operation.path == "/status/status"

            override fun mapToObjectModification(
                contributor: SimpleContributor,
                operation: PatchOperation,
                objectMapper: ObjectMapper,
            ): IntegrationModification<IntegrationStatusValues> {
                val memberValue = objectMapper.treeToValue(operation.value, IntegrationStatusValues::class.java)
                    ?: throw IllegalArgumentException("Not supported value: ${operation.value}. Supported values: [${IntegrationStatusValues.values()}]")
                return ModifyIntegrationStatus(memberValue)

            }
        },
    ),
}