package com.angorasix.projects.management.integrations.infrastructure.integrations.strategies

import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.SourceConfigurations
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.SourceType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.WebClient

class IntegrationConstants private constructor() {
    companion object {
        const val REQUEST_ATTRIBUTE_AUTHORIZATION_USER_TOKEN =
            "ANGORASIX_REQUEST_AUTHORIZATION-TOKEN"
        const val ACCESS_TOKEN_CONFIG_PARAM = "accessToken"
        const val ACCESS_USER_CONFIG_PARAM = "user"

        const val TRELLO_TOKEN_BODY_FIELD = "token"
    }
}

class WebClientStrategies {
    // default
    val logger: Logger = LoggerFactory.getLogger(WebClientStrategies::class.java)

    companion object {
        fun trelloWebClient(sourceConfigs: SourceConfigurations): WebClient {
            val trelloApiKey =
                sourceConfigs.sourceConfigs[SourceType.TRELLO.key]?.strategyConfigs?.get(
                    "apiKey",
                )
                    ?: throw IllegalArgumentException("trello apiKey config is required")

            return WebClient
                .builder()
                .defaultUriVariables(mapOf("key" to trelloApiKey))
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .filter { request, next ->
                    val userToken =
                        request.attribute(
                            IntegrationConstants.REQUEST_ATTRIBUTE_AUTHORIZATION_USER_TOKEN,
                        )
                    next.exchange(
                        ClientRequest
                            .from(request)
                            .header(
                                HttpHeaders.AUTHORIZATION,
                                "OAuth oauth_consumer_key=\"$trelloApiKey\", oauth_token=\"${userToken.get()}\"",
                            ).build(),
                    )
                }.build()
        }
    }
}
