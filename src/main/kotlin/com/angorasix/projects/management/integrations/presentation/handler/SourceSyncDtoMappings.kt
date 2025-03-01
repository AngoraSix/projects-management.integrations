package com.angorasix.projects.management.integrations.presentation.handler

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.presentation.dto.convertToDto
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAsset
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAssetSyncEvent
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationStatus
import com.angorasix.projects.management.integrations.domain.integration.asset.SourceAssetData
import com.angorasix.projects.management.integrations.domain.integration.asset.SourceAssetEstimationData
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSync
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncEvent
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncStatus
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncStatusStep
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.api.ApiConfigs
import com.angorasix.projects.management.integrations.presentation.dto.IntegrationAssetDto
import com.angorasix.projects.management.integrations.presentation.dto.IntegrationAssetStatusDto
import com.angorasix.projects.management.integrations.presentation.dto.IntegrationAssetSyncEventDto
import com.angorasix.projects.management.integrations.presentation.dto.SourceAssetDataDto
import com.angorasix.projects.management.integrations.presentation.dto.SourceAssetEstimationDataDto
import com.angorasix.projects.management.integrations.presentation.dto.SourceSyncDto
import com.angorasix.projects.management.integrations.presentation.dto.SourceSyncEventDto
import com.angorasix.projects.management.integrations.presentation.dto.SourceSyncStatusDto
import com.angorasix.projects.management.integrations.presentation.dto.SourceSyncStatusStepDto
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
    isIntegrationActive: Boolean,
): SourceSyncDto =
    SourceSyncDto(
        source,
        integrationId,
        status.convertToDto(),
        events.map { it.convertToDto() },
        sourceStrategyStateData,
        id,
        assets.map { it.convertToDto() },
    ).resolveHypermedia(
        contributor,
        this,
        apiConfigs,
        request,
        isIntegrationActive,
    )

fun SourceSyncEvent.convertToDto(): SourceSyncEventDto = SourceSyncEventDto(type, eventInstant)

fun SourceSyncStatus.convertToDto(): SourceSyncStatusDto =
    SourceSyncStatusDto(
        status,
        steps.map {
            it.convertToDto()
        },
    )

fun SourceSyncStatusStep.convertToDto(): SourceSyncStatusStepDto =
    SourceSyncStatusStepDto(stepKey, requiredDataForStep.map { it.convertToDto() }, responseData)

fun IntegrationAsset.convertToDto(): IntegrationAssetDto =
    IntegrationAssetDto(
        id,
        integrationStatus.convertToDto(),
        sourceData.convertToDto(),
        source,
        integrationId,
        sourceSyncId,
    )

fun SourceAssetData.convertToDto(): SourceAssetDataDto =
    SourceAssetDataDto(
        id,
        type,
        title,
        description,
        dueInstant,
        assigneeIds,
        done,
        estimations?.convertToDto(),
    )

fun IntegrationAssetSyncEvent.convertToDto(): IntegrationAssetSyncEventDto =
    IntegrationAssetSyncEventDto(
        type.name,
        syncEventId,
        eventInstant.toString(),
    )

fun IntegrationStatus.convertToDto(): IntegrationAssetStatusDto =
    IntegrationAssetStatusDto(
        events.map {
            it.convertToDto()
        },
    )

fun SourceAssetEstimationData.convertToDto(): SourceAssetEstimationDataDto =
    SourceAssetEstimationDataDto(
        caps,
        strategy,
        effort,
        complexity,
        industry,
        industryModifier,
        moneyPayment,
    )
