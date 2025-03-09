package com.angorasix.projects.management.integrations.domain.integration.sourcesync.inputs

import com.angorasix.commons.domain.inputs.InlineFieldSpec

data class SourceSyncMappingsUsersInput(
    val inputs: List<InlineFieldSpec>,
    val source: String,
)
