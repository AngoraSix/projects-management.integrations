package com.angorasix.projects.management.integrations.application.strategies

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.domain.projectmanagement.integrations.Source
import com.angorasix.projects.management.integrations.domain.integration.configuration.Integration
import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationConfig
import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationStatus
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.SourceConfigurations
import com.angorasix.projects.management.integrations.infrastructure.integrations.dto.TrelloMemberDto
import com.angorasix.projects.management.integrations.infrastructure.integrations.strategies.IntegrationConstants
import com.angorasix.projects.management.integrations.infrastructure.integrations.strategies.IntegrationConstants.Companion.ACCESS_TOKEN_CONFIG_PARAM
import com.angorasix.projects.management.integrations.infrastructure.integrations.strategies.IntegrationConstants.Companion.ACCESS_USER_CONFIG_PARAM
import com.angorasix.projects.management.integrations.infrastructure.integrations.strategies.IntegrationConstants.Companion.TRELLO_TOKEN_BODY_FIELD
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.reactive.function.client.WebClient

interface RegistrationStrategy {
    suspend fun processIntegrationRegistration(
        integrationData: Integration,
        requestingContributor: SimpleContributor,
        existingIntegration: Integration?
    ): Integration
}

class TrelloRegistrationStrategy(
    private val trelloWebClient: WebClient,
    private val integrationConfigs: SourceConfigurations,
    private val passwordEncoder: PasswordEncoder
) : RegistrationStrategy {
    override suspend fun processIntegrationRegistration(
        integrationData: Integration,
        requestingContributor: SimpleContributor,
        existingIntegration: Integration?
    ): Integration {
        val accessToken =
            integrationData.config.sourceStrategyConfigData?.get(TRELLO_TOKEN_BODY_FIELD) as? String
                ?: throw IllegalArgumentException("trello access token body param is required for registration")
        val memberUri =
            integrationConfigs.sourceConfigs["trello"]?.strategyConfigs?.get("memberUrl")
                ?: throw IllegalArgumentException("trello memberUrl config is required for registration")

        // Call Trello to get User data
        val trelloMemberDto = trelloWebClient.get()
            .uri(memberUri)
            .attributes{attrs ->
                attrs[IntegrationConstants.REQUEST_ATTRIBUTE_AUTHORIZATION_USER_TOKEN] =
                    accessToken
            }
            .retrieve().bodyToMono(TrelloMemberDto::class.java).awaitSingle()

        return Integration(
            existingIntegration?.id,
            Source.TRELLO.value,
            integrationData.projectManagementId,
            IntegrationStatus.registered(extractStatusData(integrationData.status.sourceStrategyStatusData)),
            setOf(requestingContributor),
            IntegrationConfig(extractConfigData(passwordEncoder.encode(accessToken), trelloMemberDto)),
        )
    }

    private fun extractStatusData(data: Map<String, Any>?): Map<String, Any>? {
        return data
    }

    private fun extractConfigData(accessToken: String, userData: TrelloMemberDto): Map<String, Any> {
        return mapOf(ACCESS_TOKEN_CONFIG_PARAM to accessToken, ACCESS_USER_CONFIG_PARAM to userData)
    }
}
