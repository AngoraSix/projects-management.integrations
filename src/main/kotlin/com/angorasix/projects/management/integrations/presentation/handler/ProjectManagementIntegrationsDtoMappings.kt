package com.angorasix.projects.management.integrations.presentation.handler

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.domain.projectmanagement.integrations.Source
import com.angorasix.projects.management.integrations.domain.integration.configuration.Integration
import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationConfig
import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationStatus
import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationStatusValues
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.api.ApiConfigs
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.SourceConfigurations
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.ListIntegrationFilter
import com.angorasix.projects.management.integrations.presentation.dto.IntegrationConfigDto
import com.angorasix.projects.management.integrations.presentation.dto.IntegrationDto
import com.angorasix.projects.management.integrations.presentation.dto.IntegrationStatusDto
import org.springframework.hateoas.CollectionModel
import org.springframework.web.reactive.function.server.ServerRequest

/**
 * <p> Class containing all Dto Mapping Extensions.</p>
 *
 * @author rozagerardo
 */
fun IntegrationDto.convertToDomain(
    admins: Set<SimpleContributor>,
    projectManagementIdParam: String? = null,
): Integration {
    val checkedSource = source?.uppercase()?.let { Source.valueOf(it) }
    val checkedProjectManagementId = projectManagementIdParam ?: projectManagementId
    if (checkedSource == null || checkedProjectManagementId == null) {
        throw IllegalArgumentException(
            "Invalid Integration - source: $source - projectManagementId: $checkedProjectManagementId",
        )
    }
    val statusValue =
        status?.convertToDomain() ?: IntegrationStatus(IntegrationStatusValues.NOT_REGISTERED)
    val configValue = config?.convertToDomain() ?: IntegrationConfig(null)
    return Integration(
        checkedSource.value,
        checkedProjectManagementId,
        statusValue,
        admins,
        configValue,
    )
}

fun IntegrationStatusDto.convertToDomain(): IntegrationStatus =
    IntegrationStatus(
        status ?: IntegrationStatusValues.NOT_REGISTERED,
        expirationDate,
        sourceStrategyData,
    )

fun IntegrationConfigDto.convertToDomain(): IntegrationConfig =
    IntegrationConfig(
        sourceStrategyConfigData,
    )

fun Integration.convertToDto(
    contributor: SimpleContributor?,
    apiConfigs: ApiConfigs,
    sourceConfigurations: SourceConfigurations,
    request: ServerRequest,
): IntegrationDto =
    IntegrationDto(
        source,
        projectManagementId,
        status.convertToDto(),
        admins,
        config.convertToDto(),
        sourceSync?.convertToDto(contributor, apiConfigs, request),
        id,
    ).resolveHypermedia(
        contributor,
        this,
        apiConfigs,
        sourceConfigurations,
        request,
    )

fun IntegrationStatus.convertToDto(): IntegrationStatusDto = IntegrationStatusDto(status, expirationDate, sourceStrategyStatusData)

fun IntegrationConfig.convertToDto(): IntegrationConfigDto =
    IntegrationConfigDto(
        sourceStrategyConfigData,
    )

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
