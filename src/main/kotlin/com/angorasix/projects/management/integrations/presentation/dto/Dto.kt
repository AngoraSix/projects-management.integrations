package com.angorasix.projects.management.integrations.presentation.dto

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationStatusValues
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
