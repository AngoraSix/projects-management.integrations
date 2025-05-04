package com.angorasix.projects.management.integrations.infrastructure.domain

import com.angorasix.commons.domain.A6Contributor
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSync

class SourceSyncContext private constructor(
    val sourceSyncId: String,
    val projectManagementId: String,
    private val admins: Set<A6Contributor>,
    val configurations: SourceSyncContextConfigurations,
) {
    fun requireAdmin(contributorId: String?) {
        if (!isAdmin(contributorId)) {
            throw IllegalArgumentException("Contributor is not a SourceSync admin")
        }
    }

    private fun isAdmin(contributorId: String?): Boolean =
        (contributorId != null).and(
            admins.any {
                it.contributorId == contributorId
            },
        )

    companion object {
        fun SourceSync.context(): SourceSyncContext =
            SourceSyncContext(
                id ?: throw IllegalArgumentException("SourceSync must have an id"),
                projectManagementId,
                admins,
                SourceSyncContextConfigurations(
                    mappings.users,
                ),
            )
    }

    class SourceSyncContextConfigurations(
        val usersMappings: Map<String, String?>,
    )
}
