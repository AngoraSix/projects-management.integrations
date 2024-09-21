package com.angorasix.projects.management.integrations.application.strategies

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.domain.projectmanagement.integrations.Source
import com.angorasix.projects.management.integrations.domain.integration.configuration.Integration
import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationConfig
import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationStatus
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.SourceConfigurations
import com.angorasix.projects.management.integrations.infrastructure.integrations.dto.BoardDto
import com.angorasix.projects.management.integrations.infrastructure.integrations.strategies.WebClientConstants
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

interface RegistrationStrategy {
    suspend fun processIntegrationRegistration(
        integrationData: Integration,
        requestingContributor: SimpleContributor,
    ): Integration
}

class TrelloRegistrationStrategy(
    private val trelloWebClient: WebClient,
    private val integrationConfigs: SourceConfigurations,
) : RegistrationStrategy {
    override suspend fun processIntegrationRegistration(
        integrationData: Integration,
        requestingContributor: SimpleContributor,
    ): Integration {
        val memberBoardsUri =
            integrationConfigs.sourceConfigs["trello"]?.strategyConfigs?.get("memberBoardsUrl")
                ?: throw IllegalArgumentException("trello memberBoardsUrl config is required for registration")

        println("DEBUGEO GERGERGER")
        println(memberBoardsUri)

        val response = trelloWebClient.get()
            .uri(memberBoardsUri)
            .attributes{attrs ->
                attrs[WebClientConstants.REQUEST_ATTRIBUTE_AUTHORIZATION_USER_TOKEN] =
                    integrationData.config.sourceStrategyConfigData?.get("token")
            }
            .retrieve().bodyToMono(object : ParameterizedTypeReference<List<BoardDto>>() {})
            .awaitSingle()

        println("DEBUGEO GERGERGER 33333")
        println(response.toString())
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
