package com.angorasix.projects.management.integrations.application.strategies

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.domain.projectmanagement.integrations.Source
import com.angorasix.projects.management.integrations.domain.integration.configuration.Integration
import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationConfig
import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationStatus
import org.springframework.web.reactive.function.client.WebClient

interface RegistrationStrategy {
    fun processIntegration(integrationData: Integration, requestingContributor: SimpleContributor): Integration
}

class TrelloRegistrationStrategy(val trelloWebClient: WebClient) : RegistrationStrategy {
    override fun processIntegration(
        integrationData: Integration,
        requestingContributor: SimpleContributor,
    ): Integration {
        // call Trello using webClient and ...
        return Integration(
            Source.TRELLO.value,
            integrationData.projectManagementId,
            IntegrationStatus.registered(extractStatusData(integrationData.status.sourceStrategyStatusData)),
            setOf(requestingContributor),
            IntegrationConfig(extractConfigData(integrationData.status.sourceStrategyStatusData)),
        )
    }

    private fun extractStatusData(data: Map<String, Any>?): Map<String, Any>? {
        return data
    }

    private fun extractConfigData(data: Map<String, Any>?): Map<String, Any>? {
        return if (data?.isEmpty() == true) {
            null
        } else {
            mapOf("TEST" to "yes", "TEST2" to data.toString())
        }
    }
}
