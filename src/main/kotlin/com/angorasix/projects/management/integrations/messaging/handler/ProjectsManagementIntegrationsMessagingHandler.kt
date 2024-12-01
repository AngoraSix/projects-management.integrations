package com.angorasix.projects.management.integrations.messaging.handler

import com.angorasix.commons.infrastructure.intercommunication.dto.messaging.A6InfraMessageDto
import kotlinx.coroutines.runBlocking

/**
 * <p>
 * </p>
 *
 * @author rozagerardo
 */
class ProjectsManagementIntegrationsMessagingHandler {
    fun reprocessPendingAssets(message: A6InfraMessageDto) = runBlocking {
        if (message.topic.any()) {
            error("Not re-processing at the moment - move to DLQ")
        }
    }
}
