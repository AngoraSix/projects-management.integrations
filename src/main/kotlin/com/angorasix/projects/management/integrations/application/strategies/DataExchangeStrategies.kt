package com.angorasix.projects.management.integrations.application.strategies

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.domain.inputs.FieldSpec
import com.angorasix.commons.domain.inputs.InlineFieldSpec
import com.angorasix.commons.domain.inputs.OptionSpec
import com.angorasix.commons.domain.projectmanagement.integrations.Source
import com.angorasix.projects.management.integrations.domain.integration.configuration.Integration
import com.angorasix.projects.management.integrations.domain.integration.exchange.DataExchange
import com.angorasix.projects.management.integrations.domain.integration.exchange.DataExchangeStatus
import com.angorasix.projects.management.integrations.domain.integration.exchange.DataExchangeStatusStep
import com.angorasix.projects.management.integrations.domain.integration.exchange.DataExchangeStatusValues
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.SourceConfigurations
import com.angorasix.projects.management.integrations.infrastructure.integrations.dto.TrelloBoardDto
import com.angorasix.projects.management.integrations.infrastructure.integrations.strategies.IntegrationConstants
import com.angorasix.projects.management.integrations.infrastructure.integrations.strategies.IntegrationConstants.Companion.ACCESS_TOKEN_CONFIG_PARAM
import com.angorasix.projects.management.integrations.infrastructure.security.TokenEncryptionUtil
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.reactive.function.client.WebClient
import java.time.Instant

inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

interface DataExchangeStrategy {
    suspend fun startDataExchange(
        integration: Integration,
        requestingContributor: SimpleContributor,
    ): DataExchange
}

class TrelloDataExchangeStrategy(
    private val trelloWebClient: WebClient,
    private val integrationConfigs: SourceConfigurations,
    private val tokenEncryptionUtil: TokenEncryptionUtil,
) : DataExchangeStrategy {
    override suspend fun startDataExchange(
        integration: Integration,
        requestingContributor: SimpleContributor,
    ): DataExchange {
        val accessToken =
            tokenEncryptionUtil.decrypt(
                integration.config.sourceStrategyConfigData?.get(ACCESS_TOKEN_CONFIG_PARAM) as? String
                    ?: throw IllegalArgumentException("trello access token body param is required for data exchange"),
            )
        val memberBoardsUri =
            integrationConfigs.sourceConfigs["trello"]?.strategyConfigs?.get("memberBoardsUrl")
                ?: throw IllegalArgumentException("trello memberBoardsUrl config is required for data exchange")

        // Call Trello to get User data
        val boardsDto = trelloWebClient.get()
            .uri(memberBoardsUri)
            .attributes { attrs ->
                attrs[IntegrationConstants.REQUEST_ATTRIBUTE_AUTHORIZATION_USER_TOKEN] =
                    accessToken
            }
            .retrieve().bodyToMono(typeReference<List<TrelloBoardDto>>()).awaitSingle()

        val boardsOptions = boardsDto.map { OptionSpec(it.id, it.name) }
        val boardFieldSpec =
            InlineFieldSpec(TrelloSteps.SELECT_BOARD.value, FieldSpec.SELECT, boardsOptions)

        return DataExchange(
            integration.id
                ?: throw IllegalArgumentException("persisted Integration is required for data exchange"),
            Source.TRELLO,
            Instant.now(),
            Instant.now(),
            DataExchangeStatus(
                DataExchangeStatusValues.IN_PROGRESS,
                listOf(
                    DataExchangeStatusStep(
                        TrelloSteps.SELECT_BOARD.value,
                        listOf(boardFieldSpec),
                    ),
                ),
            ),
            setOf(requestingContributor),
            mapOf("boards" to boardsDto),
        )
    }
}

private enum class TrelloSteps(val value: String) {
    SELECT_BOARD("SELECT_BOARD"),
}
