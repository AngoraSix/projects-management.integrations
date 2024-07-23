package com.angorasix.projects.management.integrations

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
    bean<com.angorasix.projects.management.integrations.application.ProjectsManagementIntegrationsService>()
    bean<ProjectManagementIntegrationsHandler>()
    bean {
        ProjectManagementIntegrationsRouter(ref(), ref()).projectRouterFunction()
    }
}

class BeansInitializer : ApplicationContextInitializer<GenericApplicationContext> {
    override fun initialize(context: GenericApplicationContext) = com.angorasix.projects.management.integrations.beans.initialize(context)
}
