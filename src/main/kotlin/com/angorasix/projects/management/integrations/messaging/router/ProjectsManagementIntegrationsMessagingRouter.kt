package com.angorasix.projects.management.integrations.messaging.router

import com.angorasix.commons.infrastructure.intercommunication.dto.messaging.A6InfraMessageDto
import com.angorasix.projects.management.integrations.messaging.handler.ProjectsManagementIntegrationsMessagingHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * <p>
 * </p>
 *
 * @author rozagerardo
 */
@Configuration
class ProjectsManagementIntegrationsMessagingRouter(
    val handler: ProjectsManagementIntegrationsMessagingHandler,
) {
    @Bean
    fun pendingSyncing(): java.util.function.Function<A6InfraMessageDto, Unit> =
        java.util.function.Function { handler.reprocessPendingAssets(it) }

    @Bean
    fun tasksSyncingCorrespondence(): java.util.function.Function<A6InfraMessageDto, Unit> =
        java.util.function.Function { handler.processSyncingCorrespondence(it) }
}
