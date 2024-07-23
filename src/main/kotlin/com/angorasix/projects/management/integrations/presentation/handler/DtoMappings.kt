package com.angorasix.projects.management.integrations.presentation.handler

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.projects.management.integrations.domain.integration.configuration.Integration
import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationConfig
import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationStatus
import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationStatusValues
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.api.ApiConfigs
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.ListIntegrationFilter
import com.angorasix.projects.management.integrations.presentation.dto.IntegrationConfigDto
import com.angorasix.projects.management.integrations.presentation.dto.IntegrationDto
import com.angorasix.projects.management.integrations.presentation.dto.IntegrationStatusDto
import org.springframework.hateoas.CollectionModel
import org.springframework.web.reactive.function.server.ServerRequest
import java.time.Instant

/**
 * <p> Class containing all Dto Mapping Extensions.</p>
 *
 * @author rozagerardo
 */
fun IntegrationDto.convertToDomain(admins: Set<SimpleContributor>): Integration {
    if (source == null || projectManagementId == null) {
        throw IllegalArgumentException(
            "Invalid Integration -" +
                    "source: $source -" +
                    "projectManagementId: $projectManagementId",
        )
    }
    val statusValue =
        status?.convertToDomain() ?: IntegrationStatus(IntegrationStatusValues.NOT_REGISTERED)
    val configValue = config?.convertToDomain() ?: IntegrationConfig(null)
    return Integration(
        source,
        projectManagementId,
        statusValue,
        admins,
        configValue,
    )
}

fun IntegrationStatusDto.convertToDomain(): IntegrationStatus =
    IntegrationStatus(
        status, expirationDate, sourceStrategyData,
    )

fun IntegrationConfigDto.convertToDomain(): IntegrationConfig {
    return IntegrationConfig(
        sourceStrategyConfigData,
    )
}

fun Integration.convertToDto(): IntegrationDto {
    return IntegrationDto(
        source,
        projectManagementId,
        status.convertToDto(),
        admins,
        config.convertToDto(),
        id,
    )
}

fun Integration.convertToDto(
    contributor: SimpleContributor?,
    apiConfigs: ApiConfigs,
    request: ServerRequest,
): IntegrationDto {
    return IntegrationDto(
        source,
        projectManagementId,
        status.convertToDto(),
        admins,
        config.convertToDto(),
        id,
    ).resolveHypermedia(
        contributor,
        apiConfigs,
        request,
    )
}

fun IntegrationStatus.convertToDto(): IntegrationStatusDto =
    IntegrationStatusDto(
        status, expirationDate, sourceStrategyData,
    )

fun IntegrationConfig.convertToDto(): IntegrationConfigDto {
    return IntegrationConfigDto(
        sourceStrategyConfigData,
    )
}

fun List<IntegrationDto>.convertToDto(
    contributor: SimpleContributor?,
    filter: ListIntegrationFilter,
    apiConfigs: ApiConfigs,
    request: ServerRequest,
): CollectionModel<IntegrationDto> {
    // Fix this when Spring HATEOAS provides consistent support for reactive/coroutines
    val pair = generateCollectionModel()
    return pair.second.resolveHypermedia(
        contributor,
        filter,
        apiConfigs,
        request,
    )
}