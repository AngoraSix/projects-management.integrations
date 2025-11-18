package com.angorasix.projects.management.integrations.messaging.listener.router

import com.angorasix.commons.infrastructure.intercommunication.integrations.IntegrationTaskReceived
import com.angorasix.commons.infrastructure.intercommunication.messaging.A6InfraMessageDto
import com.angorasix.commons.infrastructure.intercommunication.tasks.TasksSyncingCorrespondenceProcessed
import com.angorasix.projects.management.integrations.messaging.listener.handler.ProjectsManagementIntegrationsMessagingHandler
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
    // Revert this when this is GA: https://github.com/spring-cloud/spring-cloud-function/issues/124

    @Bean
    fun pendingSyncing(): java.util.function.Function<A6InfraMessageDto<IntegrationTaskReceived>, Unit> =
        java.util.function.Function { handler.reprocessPendingAssets(it) }

    @Bean
    fun tasksSyncingCorrespondence(): java.util.function.Function<A6InfraMessageDto<TasksSyncingCorrespondenceProcessed>, Unit> =
        java.util.function.Function { handler.processSyncingCorrespondence(it) }
}
