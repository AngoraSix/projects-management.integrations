package com.angorasix.projects.management.integrations.infrastructure.integrations.strategies

import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.SourceConfigurations
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.WebClient

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
    val trelloApiSecret = integrationConfigs.sourceConfigs["trello"]?.strategyConfigs?.get("apiSecret")
        ?: throw IllegalArgumentException("trello apiSecret config is required")
    return WebClient.builder()
        .filter { request, next ->
            next.exchange(
                ClientRequest.from(request)
                    .header(trelloApiKey, trelloApiSecret)
                    .build(),
            )
        }
        .build()
}
