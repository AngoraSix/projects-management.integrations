package com.angorasix.projects.management.integrations.infrastructure.registration.strategies

import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integration.SourceConfigurations
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
    return WebClient.builder()
        .filter { request, next ->
            next.exchange(
                ClientRequest.from(request)
                    .header(integrationConfigs.trello.apiKey, integrationConfigs.trello.apiSecret)
                    .build(),
            )
        }
        .build()
}
