package com.angorasix.projects.management.integrations.domain.integration.asset

import com.angorasix.commons.domain.projectmanagement.integrations.Source
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator

/**
 * Integration Asset Root.
 *
 * An integration asset with the third-part integration.
 *
 * @author rozagerardo
 */
data class IntegrationAsset @PersistenceCreator constructor(
    @field:Id val id: String?,
    val source: Source,
    val integrationId: String,
) {
    constructor(
        source: Source,
        integrationId: String,
    ) : this(
        null,
        source,
        integrationId,
    )
}
