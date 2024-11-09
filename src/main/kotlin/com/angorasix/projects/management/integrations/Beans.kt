package com.angorasix.projects.management.integrations

import com.angorasix.commons.domain.projectmanagement.integrations.Source
import com.angorasix.projects.management.integrations.application.DataExchangeService
import com.angorasix.projects.management.integrations.application.ProjectsManagementIntegrationsService
import com.angorasix.projects.management.integrations.application.strategies.DataExchangeStrategy
import com.angorasix.projects.management.integrations.application.strategies.RegistrationStrategy
import com.angorasix.projects.management.integrations.application.strategies.TrelloDataExchangeStrategy
import com.angorasix.projects.management.integrations.application.strategies.TrelloRegistrationStrategy
import com.angorasix.projects.management.integrations.infrastructure.integrations.strategies.trelloWebClient
import com.angorasix.projects.management.integrations.infrastructure.security.ProjectManagementIntegrationsSecurityConfiguration
import com.angorasix.projects.management.integrations.presentation.handler.DataExchangeHandler
import com.angorasix.projects.management.integrations.presentation.handler.ProjectManagementIntegrationsHandler
import com.angorasix.projects.management.integrations.presentation.router.ProjectManagementIntegrationsRouter
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.support.GenericApplicationContext
import org.springframework.context.support.beans

val beans = beans {
    bean {
        ProjectManagementIntegrationsSecurityConfiguration().passwordEncoder()
    }
    bean {
        ProjectManagementIntegrationsSecurityConfiguration().tokenEncryptionUtils(ref())
    }
    bean {
        ProjectManagementIntegrationsSecurityConfiguration().springSecurityFilterChain(ref())
    }
    bean {
        val strategies = mapOf(
            Source.TRELLO to ref<RegistrationStrategy>("trelloRegistrationStrategy"),
        )
        ProjectsManagementIntegrationsService(ref(), ref(), strategies)
    }
    bean<ProjectManagementIntegrationsHandler>()
    bean<DataExchangeHandler>()
    bean {
        val strategies = mapOf(
            Source.TRELLO to ref<DataExchangeStrategy>("trelloDataExchangeStrategy"),
        )
        DataExchangeService(ref(), ref(), strategies)
    }
    bean {
        ProjectManagementIntegrationsRouter(ref(), ref(), ref()).projectRouterFunction()
    }
    // Strategies Infrastructure
    bean("trelloWebClient") {
        trelloWebClient(ref())
    }

    // Strategies Implementations
    bean("trelloRegistrationStrategy") {
        TrelloRegistrationStrategy(ref("trelloWebClient"), ref(), ref())
    }
    bean("trelloDataExchangeStrategy") {
        TrelloDataExchangeStrategy(ref("trelloWebClient"), ref(), ref())
    }
}

class BeansInitializer : ApplicationContextInitializer<GenericApplicationContext> {
    override fun initialize(context: GenericApplicationContext) =
        com.angorasix.projects.management.integrations.beans.initialize(context)
}
