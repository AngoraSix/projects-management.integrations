package com.angorasix.projects.management.integrations.application

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.domain.projectmanagement.integrations.Source
import com.angorasix.projects.management.integrations.application.strategies.DataExchangeStrategy
import com.angorasix.projects.management.integrations.domain.integration.exchange.DataExchange
import com.angorasix.projects.management.integrations.domain.integration.exchange.DataExchangeRepository

/**
 *
 *
 * @author rozagerardo
 */
class DataExchangeService(
    private val repository: DataExchangeRepository,
    private val integrationsService: ProjectsManagementIntegrationsService,
    private val dataExchangeStrategies: Map<Source, DataExchangeStrategy>,
) {
    suspend fun createDataExchange(
        integrationId: String,
        requestingContributor: SimpleContributor,
    ): DataExchange? {
        val integration =
            integrationsService.findSingleIntegration(integrationId, requestingContributor)
        return integration?.let {
            val source = Source.valueOf(integration.source.uppercase())
            val dataExchange = dataExchangeStrategies[source]?.startDataExchange(
                integration,
                requestingContributor,
            )
            dataExchange?.let { repository.save(it) }
        }
    }
}
