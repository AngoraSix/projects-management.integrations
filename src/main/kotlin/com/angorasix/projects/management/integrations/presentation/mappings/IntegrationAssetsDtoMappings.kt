package com.angorasix.projects.management.integrations.presentation.mappings

import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAsset
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAssetStatus
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAssetSyncEvent
import com.angorasix.projects.management.integrations.domain.integration.asset.SourceAssetData
import com.angorasix.projects.management.integrations.domain.integration.asset.SourceAssetEstimationData
import com.angorasix.projects.management.integrations.presentation.dto.IntegrationAssetDto
import com.angorasix.projects.management.integrations.presentation.dto.IntegrationAssetStatusDto
import com.angorasix.projects.management.integrations.presentation.dto.IntegrationAssetSyncEventDto
import com.angorasix.projects.management.integrations.presentation.dto.SourceAssetDataDto
import com.angorasix.projects.management.integrations.presentation.dto.SourceAssetEstimationDataDto

/**
 * <p> Convert SourceSyncDto to Domain - only for initial setup, there is no update</p>
 *
 * @author rozagerardo
 */
fun IntegrationAsset.convertToDto(): IntegrationAssetDto =
    IntegrationAssetDto(
        id,
        integrationAssetStatus.convertToDto(),
        sourceData.convertToDto(),
        source,
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

fun IntegrationAssetStatus.convertToDto(): IntegrationAssetStatusDto =
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
