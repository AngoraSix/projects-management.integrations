package com.angorasix.projects.management.integrations.infrastructure.integrations.strategies

import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.SourceConfigurations
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.resources.ConnectionProvider
import kotlin.time.Duration


class WebClientConstants private constructor() {
    companion object {
        const val REQUEST_ATTRIBUTE_AUTHORIZATION_USER_TOKEN = "ANGORASIX_REQUEST_AUTHORIZATION-TOKEN"
    }
}


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
    println("DEBUGEO GERGERGER 2222")
    println(integrationConfigs.sourceConfigs["trello"]?.strategyConfigs)
    println(integrationConfigs.sourceConfigs["trello"]?.strategyConfigs?.get("memberBoardsUrl"))

//    val trelloApiSecret = integrationConfigs.sourceConfigs["trello"]?.strategyConfigs?.get("apiSecret")
//        ?: throw IllegalArgumentException("trello apiSecret config is required")
    return WebClient.builder()
        .defaultUriVariables(mapOf("key" to trelloApiKey))
        .defaultHeader(HttpHeaders.ACCEPT, "application/json")
        .filter { request, next ->
            val userToken = request.attribute(WebClientConstants.REQUEST_ATTRIBUTE_AUTHORIZATION_USER_TOKEN)
            println("RERERERERERE")
            println("OAuth oauth_consumer_key=\"%s\", oauth_token=\"%s\"".format(trelloApiKey, userToken.get()))
            next.exchange(
                ClientRequest.from(request)
                    .header(HttpHeaders.AUTHORIZATION, "OAuth oauth_consumer_key=\"%s\", oauth_token=\"%s\"".format(trelloApiKey, userToken.get()))
//                    .header(HttpHeaders.AUTHORIZATION, "OAuth oauth_consumer_key=\"%s\", oauth_token=\"%s\"".format(trelloApiKey, userToken.get()))
                    .build(),
            )

        }
        .build()
}
