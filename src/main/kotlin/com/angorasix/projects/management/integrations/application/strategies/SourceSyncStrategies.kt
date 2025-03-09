package com.angorasix.projects.management.integrations.application.strategies

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAsset
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSync
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceUser
import org.springframework.core.ParameterizedTypeReference

inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

interface SourceSyncStrategy {
    suspend fun resolveSourceSyncRegistration(
        sourceSyncData: SourceSync,
        requestingContributor: SimpleContributor,
        existingSourceSync: SourceSync?,
    ): SourceSync

    suspend fun isReadyForSyncing(
        sourceSync: SourceSync,
        requestingContributor: SimpleContributor,
    ): Boolean

    suspend fun configureNextStepData(
        sourceSync: SourceSync,
        requestingContributor: SimpleContributor,
    ): SourceSync

    suspend fun triggerSourceSync(
        sourceSync: SourceSync,
        requestingContributor: SimpleContributor,
        syncEventId: String,
    ): List<IntegrationAsset>

    suspend fun obtainUsersMatchOptions(
        sourceSync: SourceSync,
        requestingContributor: SimpleContributor,
    ): List<SourceUser>
}
