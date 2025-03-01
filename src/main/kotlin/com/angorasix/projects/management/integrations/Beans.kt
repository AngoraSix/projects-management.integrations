package com.angorasix.projects.management.integrations

import com.angorasix.commons.domain.projectmanagement.integrations.Source
import com.angorasix.projects.management.integrations.application.IntegrationAssetService
import com.angorasix.projects.management.integrations.application.IntegrationsService
import com.angorasix.projects.management.integrations.application.SourceSyncService
import com.angorasix.projects.management.integrations.application.strategies.RegistrationStrategy
import com.angorasix.projects.management.integrations.application.strategies.SourceSyncStrategy
import com.angorasix.projects.management.integrations.application.strategies.TrelloRegistrationStrategy
import com.angorasix.projects.management.integrations.application.strategies.source.TrelloSourceSyncStrategy
import com.angorasix.projects.management.integrations.infrastructure.integrations.strategies.WebClientStrategies
import com.angorasix.projects.management.integrations.infrastructure.security.ProjectManagementIntegrationsSecurityConfiguration
import com.angorasix.projects.management.integrations.messaging.handler.ProjectsManagementIntegrationsMessagingHandler
import com.angorasix.projects.management.integrations.presentation.handler.ProjectManagementIntegrationsHandler
import com.angorasix.projects.management.integrations.presentation.handler.SourceSyncHandler
import com.angorasix.projects.management.integrations.presentation.router.ProjectManagementIntegrationsRouter
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.support.GenericApplicationContext
import org.springframework.context.support.beans

val beans =
    beans {
        bean {
            ProjectManagementIntegrationsSecurityConfiguration.passwordEncoder()
        }
        bean {
            ProjectManagementIntegrationsSecurityConfiguration.tokenEncryptionUtils(ref())
        }
        bean {
            ProjectManagementIntegrationsSecurityConfiguration.springSecurityFilterChain(ref())
        }
        bean {
            val strategies =
                mapOf(
                    Source.TRELLO to ref<RegistrationStrategy>("trelloRegistrationStrategy"),
                )
            IntegrationsService(ref(), ref(), strategies, ref())
        }
        bean<ProjectManagementIntegrationsHandler>()
        bean<SourceSyncHandler>()
        bean {
            val strategies =
                mapOf(
                    Source.TRELLO to ref<SourceSyncStrategy>("trelloSourceSyncStrategy"),
                )
            SourceSyncService(ref(), ref(), strategies, ref())
        }
        bean<ProjectsManagementIntegrationsMessagingHandler>()
        bean<IntegrationAssetService>()
        bean {
            ProjectManagementIntegrationsRouter(ref(), ref(), ref()).projectRouterFunction()
        }
        // Strategies Infrastructure
        bean("trelloWebClient") {
            WebClientStrategies.trelloWebClient(ref())
        }

        // Strategies Implementations
        bean("trelloRegistrationStrategy") {
            TrelloRegistrationStrategy(ref("trelloWebClient"), ref(), ref())
        }
        bean("trelloSourceSyncStrategy") {
            TrelloSourceSyncStrategy(ref("trelloWebClient"), ref(), ref(), ref())
        }
    }

class BeansInitializer : ApplicationContextInitializer<GenericApplicationContext> {
    override fun initialize(context: GenericApplicationContext) =
        com.angorasix.projects.management.integrations.beans
            .initialize(context)
}
