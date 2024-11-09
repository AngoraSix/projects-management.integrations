//package com.angorasix.projects.management.integrations.presentation.strategies
//
//import com.angorasix.commons.domain.SimpleContributor
//import com.angorasix.commons.domain.projectmanagement.integrations.Source
//import com.angorasix.projects.management.integrations.domain.integration.configuration.Integration
//import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationConfig
//import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationStatus
//import com.angorasix.projects.management.integrations.domain.integration.exchange.DataExchange
//import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.api.ApiConfigs
//import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.SourceConfigurations
//import com.angorasix.projects.management.integrations.infrastructure.integrations.dto.TrelloMemberDto
//import com.angorasix.projects.management.integrations.infrastructure.integrations.strategies.IntegrationConstants
//import com.angorasix.projects.management.integrations.infrastructure.integrations.strategies.IntegrationConstants.Companion.ACCESS_TOKEN_CONFIG_PARAM
//import com.angorasix.projects.management.integrations.infrastructure.integrations.strategies.IntegrationConstants.Companion.ACCESS_USER_CONFIG_PARAM
//import com.angorasix.projects.management.integrations.infrastructure.integrations.strategies.IntegrationConstants.Companion.TRELLO_TOKEN_BODY_FIELD
//import com.angorasix.projects.management.integrations.infrastructure.security.TokenEncryptionUtil
//import com.angorasix.projects.management.integrations.presentation.dto.DataExchangeDto
//import com.angorasix.projects.management.integrations.presentation.utils.uriBuilder
//import kotlinx.coroutines.reactor.awaitSingle
//import org.springframework.hateoas.AffordanceModel
//import org.springframework.hateoas.Link
//import org.springframework.hateoas.mediatype.Affordances
//import org.springframework.security.crypto.password.PasswordEncoder
//import org.springframework.web.reactive.function.client.WebClient
//import org.springframework.web.reactive.function.server.ServerRequest
//
//interface PresentationStrategy {
//    suspend fun processDataExchangeDto(
//        request: ServerRequest,
//        dataExchange: DataExchange,
//        dataExchangeDto: DataExchangeDto,
//        requestingContributor: SimpleContributor,
//        integration: Integration
//    ): DataExchangeDto
//}
//
//class TrelloDataExchangeStrategy(
//    private val apiConfigs: ApiConfigs,
//) : PresentationStrategy {
//    override suspend fun processDataExchangeDto(
//        request: ServerRequest,
//        dataExchange: DataExchange,
//        dataExchangeDto: DataExchangeDto,
//        requestingContributor: SimpleContributor,
//        integration: Integration
//    ): DataExchangeDto {
//        if (integration.isAdmin(requestingContributor.contributorId)){
//            val createDataExchangeRoute = apiConfigs.routes.createDataExchange
//            val createDataExchangeActionName = apiConfigs.integrationActions.importData
//            val createDataExchangeLink = Link.of(
//                uriBuilder(request).path(createDataExchangeRoute.resolvePath()).build()
//                    .toUriString(),
//            ).withTitle(createDataExchangeActionName).withName(createDataExchangeActionName)
//                .withRel(createDataExchangeActionName).expand(integration.id)
//            val registerAllAffordanceLink =
//                Affordances.of(createDataExchangeLink).afford(createDataExchangeRoute.method)
//                    // .withInput(...maybe when dynamic options are supported: https://docs.spring.io/spring-hateoas/docs/current/reference/html/#mediatypes.hal-forms.options
////                        AdminContributorRequirements::class.java)
//                    .withName(createDataExchangeActionName).toLink()
//            add(registerAllAffordanceLink)
//            dataExchange.sourceStrategyStateData
//
//
//        }
//
//    }
//
//    private fun extractStatusData(data: Map<String, Any>?): Map<String, Any>? {
//        return data
//    }
//
//    private fun extractConfigData(accessToken: String, userData: TrelloMemberDto): Map<String, Any> {
//        return mapOf(ACCESS_TOKEN_CONFIG_PARAM to accessToken, ACCESS_USER_CONFIG_PARAM to userData)
//    }
//}
