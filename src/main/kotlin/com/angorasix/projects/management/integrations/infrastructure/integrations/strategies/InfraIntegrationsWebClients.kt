package com.angorasix.projects.management.integrations.infrastructure.integrations.strategies

import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.SourceConfigurations
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
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

/* default */
val logger: Logger = LoggerFactory.getLogger(
    "com.angorasix.projects.management.integrations.infrastructure.integrations.strategies.InfraIntegrationsWebClients",
)
//    @Bean
//    fun oauth2WebClient(authorizedClientManager: ReactiveOAuth2AuthorizedClientManager): WebClient {
//        val oauth2Client = ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
//        return WebClient.builder()
//            .filter(oauth2Client)
//            .build()
//    }

@Bean
fun trelloWebClient(integrationConfigs: SourceConfigurations): WebClient {
    val trelloApiKey = integrationConfigs.sourceConfigs["trello"]?.strategyConfigs?.get("apiKey")
        ?: throw IllegalArgumentException("trello apiKey config is required")

//    val trelloApiSecret = integrationConfigs.sourceConfigs["trello"]?.strategyConfigs?.get("apiSecret")
//        ?: throw IllegalArgumentException("trello apiSecret config is required")
    return WebClient.builder()
        .defaultUriVariables(mapOf("key" to trelloApiKey))
        .defaultHeader(HttpHeaders.ACCEPT, "application/json")
        .filter { request, next ->
            val userToken =
                request.attribute(IntegrationConstants.REQUEST_ATTRIBUTE_AUTHORIZATION_USER_TOKEN)
            // @TODO: Just for dev debugging purposes
            println(
                "OAuth oauth_consumer_key=\"%s\", oauth_token=\"%s\"".format(
                    trelloApiKey,
                    userToken.get(),
                ),
            )
            next.exchange(
                ClientRequest.from(request)
                    .header(
                        HttpHeaders.AUTHORIZATION,
                        "OAuth oauth_consumer_key=\"$trelloApiKey\", oauth_token=\"${userToken.get()}\"",
                    )
                    .build(),
            )

        }
        .build()
}
