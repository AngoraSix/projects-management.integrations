package com.angorasix.projects.management.integrations

import com.angorasix.commons.domain.projectmanagement.integrations.Source
import com.angorasix.projects.management.integrations.application.ProjectsManagementIntegrationsService
import com.angorasix.projects.management.integrations.application.strategies.RegistrationStrategy
import com.angorasix.projects.management.integrations.application.strategies.TrelloRegistrationStrategy
import com.angorasix.projects.management.integrations.infrastructure.integrations.strategies.trelloWebClient
import com.angorasix.projects.management.integrations.infrastructure.security.ProjectManagementIntegrationsSecurityConfiguration
import com.angorasix.projects.management.integrations.presentation.handler.ProjectManagementIntegrationsHandler
import com.angorasix.projects.management.integrations.presentation.router.ProjectManagementIntegrationsRouter
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.support.GenericApplicationContext
import org.springframework.context.support.beans

val beans = beans {
    bean {
        ProjectManagementIntegrationsSecurityConfiguration().springSecurityFilterChain(ref())
    }
    bean {
        val strategies = mapOf(
            Source.TRELLO to ref<RegistrationStrategy>("trelloStrategy"),
        )
        ProjectsManagementIntegrationsService(ref(), ref(), strategies)
    }
    bean<ProjectManagementIntegrationsHandler>()
    bean {
        ProjectManagementIntegrationsRouter(ref(), ref()).projectRouterFunction()
    }
    // Strategies Infrastructure
    bean("trelloWebClient") {
        trelloWebClient(ref())
    }

    // Strategies Implementations
    bean("trelloStrategy") {
        TrelloRegistrationStrategy(ref("trelloWebClient"), ref())
    }
}

class BeansInitializer : ApplicationContextInitializer<GenericApplicationContext> {
    override fun initialize(context: GenericApplicationContext) =
        com.angorasix.projects.management.integrations.beans.initialize(context)
}
