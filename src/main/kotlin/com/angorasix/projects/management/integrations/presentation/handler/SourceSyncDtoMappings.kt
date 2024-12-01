package com.angorasix.projects.management.integrations.presentation.handler

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.domain.projectmanagement.integrations.Source
import com.angorasix.commons.presentation.dto.convertToDto
import com.angorasix.projects.management.integrations.domain.integration.configuration.Integration
import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationConfig
import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationStatus
import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationStatusValues
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSync
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncEvent
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncStatus
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncStatusStep
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.api.ApiConfigs
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.SourceConfigurations
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.ListIntegrationFilter
import com.angorasix.projects.management.integrations.presentation.dto.IntegrationConfigDto
import com.angorasix.projects.management.integrations.presentation.dto.IntegrationDto
import com.angorasix.projects.management.integrations.presentation.dto.IntegrationStatusDto
import com.angorasix.projects.management.integrations.presentation.dto.SourceSyncDto
import com.angorasix.projects.management.integrations.presentation.dto.SourceSyncEventDto
import com.angorasix.projects.management.integrations.presentation.dto.SourceSyncStatusDto
import com.angorasix.projects.management.integrations.presentation.dto.SourceSyncStatusStepDto
import org.springframework.hateoas.CollectionModel
import org.springframework.web.reactive.function.server.ServerRequest

/**
 * <p> Class containing all Dto Mapping Extensions.</p>
 *
 * @author rozagerardo
 */
fun SourceSync.convertToDto(
    contributor: SimpleContributor?,
    apiConfigs: ApiConfigs,
    request: ServerRequest,
): SourceSyncDto {
    return SourceSyncDto(
        source,
        integrationId,
        status.convertToDto(),
        events.map { it.convertToDto() },
        sourceStrategyStateData,
        id,
    ).resolveHypermedia(
        contributor,
        this,
        apiConfigs,
        request,
    )
}

fun SourceSyncEvent.convertToDto(): SourceSyncEventDto =
    SourceSyncEventDto(type, eventInstant)

fun SourceSyncStatus.convertToDto(): SourceSyncStatusDto =
    SourceSyncStatusDto(status, steps.map { it.convertToDto() })

fun SourceSyncStatusStep.convertToDto(): SourceSyncStatusStepDto =
    SourceSyncStatusStepDto(stepKey, requiredDataForStep.map { it.convertToDto() }, responseData)
