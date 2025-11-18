package com.angorasix.projects.management.integrations.infrastructure.service

import com.angorasix.commons.domain.projectmanagement.integrations.Source
import com.angorasix.projects.management.integrations.application.IntegrationAssetService
import com.angorasix.projects.management.integrations.application.SourceSyncService
import com.angorasix.projects.management.integrations.application.strategies.SourceSyncStrategy
import com.angorasix.projects.management.integrations.application.strategies.source.TrelloSourceSyncStrategy
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAssetRepository
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncRepository
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.amqp.AmqpConfigurations
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.api.ApiConfigs
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.SourceConfigurations
import com.angorasix.projects.management.integrations.infrastructure.integrations.strategies.WebClientStrategies
import com.angorasix.projects.management.integrations.infrastructure.security.TokenEncryptionUtil
import com.angorasix.projects.management.integrations.messaging.listener.handler.ProjectsManagementIntegrationsMessagingHandler
import com.angorasix.projects.management.integrations.messaging.publisher.MessagePublisher
import com.angorasix.projects.management.integrations.presentation.handler.ProjectManagementIntegrationsHandler
import com.angorasix.projects.management.integrations.presentation.router.ProjectManagementIntegrationsRouter
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class ServiceConfiguration {
    @Bean
    fun sourceSyncService(
        repository: SourceSyncRepository,
        sourceConfigs: SourceConfigurations,
        @Qualifier("trelloSourceSyncStrategy") trelloStrategy: SourceSyncStrategy,
        assetsService: IntegrationAssetService,
    ): SourceSyncService {
        val strategies =
            mapOf(
                Source.TRELLO to trelloStrategy,
            )
        return SourceSyncService(repository, sourceConfigs, strategies, assetsService)
    }

    @Bean
    fun projectManagementIntegrationsHandler(
        service: SourceSyncService,
        apiConfigs: ApiConfigs,
        sourceConfigurations: SourceConfigurations,
        objectMapper: ObjectMapper,
    ) = ProjectManagementIntegrationsHandler(service, apiConfigs, sourceConfigurations, objectMapper)

    @Bean
    fun projectsManagementIntegrationsMessagingHandler(service: SourceSyncService) = ProjectsManagementIntegrationsMessagingHandler(service)

    @Bean
    fun messagePublisher(
        streamBridge: StreamBridge,
        amqpConfigs: AmqpConfigurations,
    ) = MessagePublisher(streamBridge, amqpConfigs)

    @Bean
    fun integrationAssetService(
        repository: IntegrationAssetRepository,
        messagePublisher: MessagePublisher,
    ) = IntegrationAssetService(repository, messagePublisher)

    @Bean
    fun projectManagementIntegrationsRouter(
        handler: ProjectManagementIntegrationsHandler,
        apiConfigs: ApiConfigs,
    ) = ProjectManagementIntegrationsRouter(handler, apiConfigs).projectRouterFunction()

    // Strategies Infrastructure
    @Bean("trelloWebClient")
    fun trelloWebClient(sourceConfigs: SourceConfigurations) = WebClientStrategies.trelloWebClient(sourceConfigs)

    // Source Strategies Implementations
    @Bean("trelloSourceSyncStrategy")
    fun trelloSourceSyncStrategy(
        @Qualifier("trelloWebClient") trelloWebClient: WebClient,
        sourceConfigs: SourceConfigurations,
        tokenEncryptionUtil: TokenEncryptionUtil,
        objectMapper: ObjectMapper,
    ) = TrelloSourceSyncStrategy(trelloWebClient, sourceConfigs, tokenEncryptionUtil, objectMapper)
}
