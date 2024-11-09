package com.angorasix.projects.management.integrations.domain.integration.asset

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.domain.projectmanagement.integrations.Source
import com.angorasix.projects.management.integrations.domain.integration.exchange.DataExchange
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
    val sourceType: String,
    val sourceId: String,
    val projectManagementId: String, // for a particular Project Mgmt (same user/admin could link to the same source),
    val admins: Set<SimpleContributor> = emptySet(),
    val exchanges: List<DataExchange> = emptyList(),
    val sourceStrategyConfigData: Any?, // any information used by the integration/source strategy to retrieve data
    val sourceStrategyStateData: Any?, // any information used by the integration/source strategy to manage its state
) {
    constructor(
        source: Source,
        sourceType: String,
        sourceId: String,
        projectManagementId: String,
        admins: Set<SimpleContributor> = emptySet(),
        exchanges: List<DataExchange> = emptyList(),
        sourceStrategyConfigData: Any?,
        sourceStrategyStateData: Any?,
    ) : this(
        null,
        source,
        sourceType,
        sourceId,
        projectManagementId,
        admins,
        exchanges,
        sourceStrategyConfigData,
        sourceStrategyStateData,
    )
}
