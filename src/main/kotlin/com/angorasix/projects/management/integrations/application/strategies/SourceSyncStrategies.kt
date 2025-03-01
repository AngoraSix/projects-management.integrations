package com.angorasix.projects.management.integrations.application.strategies

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAsset
import com.angorasix.projects.management.integrations.domain.integration.configuration.Integration
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSync
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceUser
import org.springframework.core.ParameterizedTypeReference

inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

interface SourceSyncStrategy {
    suspend fun configSourceSync(
        integration: Integration,
        requestingContributor: SimpleContributor,
        existingInProgressSourceSync: SourceSync?,
    ): SourceSync

    suspend fun isReadyForSyncing(
        sourceSync: SourceSync,
        integration: Integration,
        requestingContributor: SimpleContributor,
    ): Boolean

    suspend fun processModification(
        sourceSync: SourceSync,
        integration: Integration,
        requestingContributor: SimpleContributor,
    ): SourceSync

    suspend fun triggerSourceSync(
        sourceSync: SourceSync,
        integration: Integration,
        requestingContributor: SimpleContributor,
        syncEventId: String,
    ): List<IntegrationAsset>

    suspend fun obtainUsersMatchOptions(
        sourceSync: SourceSync,
        integration: Integration,
        requestingContributor: SimpleContributor,
    ): List<SourceUser>
}
