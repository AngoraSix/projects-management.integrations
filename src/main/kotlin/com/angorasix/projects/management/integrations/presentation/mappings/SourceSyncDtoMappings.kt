package com.angorasix.projects.management.integrations.presentation.mappings

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.domain.projectmanagement.integrations.Source
import com.angorasix.commons.presentation.dto.convertToDto
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSync
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncConfig
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncStatus
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncStatusStep
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.inputs.SourceSyncMappingsUsersInput
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.api.ApiConfigs
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.SourceConfigurations
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.SourceSyncFilter
import com.angorasix.projects.management.integrations.presentation.dto.SourceSyncConfigDto
import com.angorasix.projects.management.integrations.presentation.dto.SourceSyncDto
import com.angorasix.projects.management.integrations.presentation.dto.SourceSyncMappingsUsersInputCollectionModel
import com.angorasix.projects.management.integrations.presentation.dto.SourceSyncStatusDto
import com.angorasix.projects.management.integrations.presentation.dto.SourceSyncStatusStepDto
import org.springframework.hateoas.CollectionModel
import org.springframework.web.reactive.function.server.ServerRequest

/**
 * <p> Convert SourceSyncDto to Domain - only for initial setup, there is no update</p>
 *
 * @author rozagerardo
 */
fun SourceSyncDto.convertToDomain(projectManagementIdParam: String? = null): SourceSync {
    val checkedSource = source?.uppercase()?.let { Source.valueOf(it) }
    requireNotNull(checkedSource) { "Source is not supported: [$source]" }
    val checkedProjectManagementId = projectManagementIdParam ?: projectManagementId
    requireNotNull(checkedProjectManagementId) { "Can't resolve projectManagementId" }

    val configValue = config?.convertToDomain() ?: SourceSyncConfig(null)
    return SourceSync.notRegistered(
        checkedSource.value,
        checkedProjectManagementId,
        configValue,
    )
}

fun SourceSyncConfigDto.convertToDomain(): SourceSyncConfig =
    SourceSyncConfig(
        accessToken,
    )

fun SourceSync.convertToDto(
    contributor: SimpleContributor?,
    apiConfigs: ApiConfigs,
    sourceConfigurations: SourceConfigurations,
    request: ServerRequest,
): SourceSyncDto =
    SourceSyncDto(
        source,
        projectManagementId,
        status.convertToDto(),
        config.convertToDto(),
        assets.map { it.convertToDto() },
        id,
    ).resolveHypermedia(
        contributor,
        this,
        apiConfigs,
        sourceConfigurations,
        request,
    )

fun SourceSyncStatus.convertToDto(): SourceSyncStatusDto =
    SourceSyncStatusDto(
        status,
        expirationDate,
    )

fun SourceSyncConfig.convertToDto(): SourceSyncConfigDto =
    SourceSyncConfigDto(
        steps.map {
            it.convertToDto()
        },
    )

fun List<SourceSyncDto>.convertToDto(
    contributor: SimpleContributor?,
    filter: SourceSyncFilter,
    apiConfigs: ApiConfigs,
    request: ServerRequest,
): CollectionModel<SourceSyncDto> {
    // Fix this when Spring HATEOAS provides consistent support for reactive/coroutines
    val pair = generateCollectionModel(SourceSyncDto::class.java)
    return pair.second.resolveHypermedia(
        contributor,
        filter,
        apiConfigs,
        request,
    )
}

fun SourceSyncStatusStep.convertToDto(): SourceSyncStatusStepDto =
    SourceSyncStatusStepDto(stepKey, requiredDataForStep.map { it.convertToDto() }, responseData)

fun SourceSyncMappingsUsersInput.convertToDto(
    contributor: SimpleContributor?,
    apiConfigs: ApiConfigs,
    request: ServerRequest,
): SourceSyncMappingsUsersInputCollectionModel {
    // Fix this when Spring HATEOAS provides consistent support for reactive/coroutines
    val inputDtos = inputs.map { it.convertToDto() }
    return SourceSyncMappingsUsersInputCollectionModel(source, inputDtos).resolveHypermedia(
        contributor,
        apiConfigs,
        request,
    )
}
