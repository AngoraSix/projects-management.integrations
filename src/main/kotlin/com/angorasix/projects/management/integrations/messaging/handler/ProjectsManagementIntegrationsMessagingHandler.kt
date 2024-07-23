package com.angorasix.notifications.messaging.handler

import com.angorasix.commons.infrastructure.intercommunication.A6DomainResource
import com.angorasix.commons.infrastructure.intercommunication.A6InfraTopics
import com.angorasix.commons.infrastructure.intercommunication.messaging.dto.A6InfraMessageDto
import com.angorasix.projects.management.integrations.application.ProjectsManagementIntegrationsService
import com.angorasix.projects.management.integrations.presentation.dto.IntegrationDto
import com.angorasix.projects.management.integrations.presentation.handler.convertToDomain
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking

val integrationDtoListType = object : TypeReference<List<IntegrationDto>>() {}

/**
 * <p>
 * </p>
 *
 * @author rozagerardo
 */
class ProjectsManagementIntegrationsMessagingHandler(
    private val projectsManagementIntegrationsService: ProjectsManagementIntegrationsService,
    private val objectMapper: ObjectMapper,
) {
    //THIS WAS ALREADY DISABLED
//    fun projectManagementIntegrationsUpdate(message: A6InfraMessageDto) = runBlocking {
//        val source = getSourceFromValue(message.objectId)
//        if (message.topic == A6InfraTopics.MGMT_TASKS_UPDATE.value && message.targetType == A6DomainResource.PROJECT_MANAGEMENT && message.objectType == A6DomainResource.PROJECT_MANAGEMENT_INTEGRATION_SOURCE.value && source != null) {
//
//            val projectManagementId = message.targetId
//            val requestingContributor = message.requestingContributor
//            val integrations = message.extractIntegrationDtos(objectMapper, projectManagementId)
//                .map { it.convertToDomain(setOf(requestingContributor)) }
//            projectsManagementIntegrationsService.projectManagementIntegrationsBatchUpdate(
//                projectManagementId,
//                source,
//                requestingContributor,
//                integrations,
//            )//?.launchIn(this) // required
//        }
//    }
}



// THIS WAS THE ONE ENABLED
//private fun A6InfraMessageDto.extractIntegrationDtos(
//    objectMapper: ObjectMapper,
//    projectManagementId: String,
//): List<IntegrationDto> {
//    val integrationsJson = objectMapper.writeValueAsString(messageData["integrations"])
//    val partiallyPopulatedIntegrations = objectMapper.readValue(integrationsJson, integrationDtoListType)
//    return partiallyPopulatedIntegrations.map {
//        IntegrationDto(
//            projectManagementId,
//            emptySet(),
//            it.assignees,
//            it.title,
//            it.description,
//            it.estimation,
//            it.sourceIntegrationId,
//            null,
//        )
//    }
//}
//
//private fun getSourceFromValue(value: String) =
//    Source.values().find { it.value.equals(value, ignoreCase = true) }

