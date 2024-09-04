package com.angorasix.projects.management.integrations.domain.integration.asset

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.domain.projectmanagement.integrations.Source
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceCreator
import org.springframework.data.mongodb.core.mapping.Document

/**
 * <p>
 *     Root entity defining the Project Management Integration data.
 * </p>
 *
 * @author rozagerardo
 */
@Document
class IntegrationAsset @PersistenceCreator public constructor(
    @field:Id val id: String?,
    val source: Source,
    val projectManagementId: String, // for a particular Project Mgmt (same user/admin could link to the same source),
    val admins: Set<SimpleContributor> = emptySet(),
    val sourceStrategyConfigData: Any?, // any information used by the integration/source strategy to retrieve data
    val sourceStrategyStateData: Any?, // any information used by the integration/source strategy to manage its state
) {
    constructor(
        source: Source,
        projectManagementId: String,
        admins: Set<SimpleContributor> = emptySet(),
        sourceStrategyConfigData: Any?,
        sourceStrategyStateData: Any?,
    ) : this(
        null,
        source,
        projectManagementId,
        admins,
        sourceStrategyConfigData,
        sourceStrategyStateData,
    )
}
