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
    fun pendingSyncing(): (A6InfraMessageDto) -> Unit = { handler.reprocessPendingAssets(it) }

    @Bean
    fun tasksSyncingCorrespondence(): (A6InfraMessageDto) -> Unit = { handler.processSyncingCorrespondence(it) }
}
