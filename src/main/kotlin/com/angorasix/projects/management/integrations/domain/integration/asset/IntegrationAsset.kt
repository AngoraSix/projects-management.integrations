package com.angorasix.projects.management.integrations.domain.integration.exchange

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.domain.inputs.InlineFieldSpec
import com.angorasix.commons.domain.projectmanagement.integrations.Source
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import java.time.Instant

/**
 * Data Exchange Root.
 *
 * An exchange of data with the third-part integration.
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
