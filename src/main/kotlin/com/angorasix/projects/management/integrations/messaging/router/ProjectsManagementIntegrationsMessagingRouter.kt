package com.angorasix.notifications.messaging.router

import com.angorasix.commons.infrastructure.intercommunication.messaging.dto.A6InfraMessageDto
import com.angorasix.notifications.messaging.handler.ProjectsManagementIntegrationsMessagingHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * <p>
 * </p>
 *
 * @author rozagerardo
 */
//@Configuration // spring-cloud-streams is not prepared to handle Kotlin DSL beans: https://github.com/spring-cloud/spring-cloud-stream/issues/2025
//class ProjectsManagementIntegrationsMessagingRouter(val handler: ProjectsManagementIntegrationsMessagingHandler) {
//    @Bean
//    fun mgmtupdate(): (A6InfraMessageDto) -> Unit = { handler.projectManagementIntegrationsUpdate(it) }
//}
