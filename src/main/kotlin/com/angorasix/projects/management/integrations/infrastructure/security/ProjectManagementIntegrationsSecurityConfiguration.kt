package com.angorasix.projects.management.integrations.infrastructure.security

import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integration.SourceConfigurations
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.WebClient

/**
 *
 *
 * All Spring Security configuration.
 *
 *
 * @author rozagerardo
 */
class ProjectManagementIntegrationsSecurityConfiguration {

    /**
     *
     *
     * Security Filter Chain setup.
     *
     *
     * @param http Spring's customizable ServerHttpSecurity bean
     * @return fully configured SecurityWebFilterChain
     */
    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http.authorizeExchange { exchanges: ServerHttpSecurity.AuthorizeExchangeSpec ->
            exchanges
                .pathMatchers(
                    HttpMethod.GET,
                    "/management-integrations/**",
                ).permitAll()
                .anyExchange().authenticated()
        }.oauth2ResourceServer { oauth2 ->
            oauth2.jwt(Customizer.withDefaults())
        }
//            .oauth2Client(Customizer.withDefaults())
        return http.build()
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
        return WebClient.builder()
            .filter { request, next ->
                next.exchange(
                    ClientRequest.from(request)
                        .header(integrationConfigs.trello.apiKey, integrationConfigs.trello.apiSecret)
                        .build()
                )
            }
            .build()
    }
}
