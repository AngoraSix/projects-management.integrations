package com.angorasix.projects.management.integrations.presentation.handler

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.infrastructure.constants.AngoraSixInfrastructure
import com.angorasix.commons.reactive.presentation.error.resolveBadRequest
import com.angorasix.commons.reactive.presentation.error.resolveNotFound
import com.angorasix.projects.management.integrations.application.ProjectsManagementIntegrationsService
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.api.ApiConfigs
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.SourceConfigurations
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.ListIntegrationFilter
import com.angorasix.projects.management.integrations.presentation.dto.IntegrationDto
import org.springframework.hateoas.IanaLinkRelations
import org.springframework.hateoas.MediaTypes
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.created
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import java.net.URI

/**
 * ProjectManagementIntegration Handler (Controller) containing all handler functions
 * related to ProjectManagementIntegration endpoints.
 *
 * @author rozagerardo
 */
class ProjectManagementIntegrationsHandler(
    private val service: ProjectsManagementIntegrationsService,
    private val apiConfigs: ApiConfigs,
    private val sourceConfigurations: SourceConfigurations,
) {

    /**
     * Handler for the Get Single ProjectManagementIntegration endpoint,
     * retrieving a Mono with the requested ProjectManagementIntegration.
     *
     * @param request - HTTP `ServerRequest` object
     * @return the `ServerResponse`
     */
    suspend fun getIntegration(request: ServerRequest): ServerResponse {
        val requestingContributor =
            request.attributes()[AngoraSixInfrastructure.REQUEST_ATTRIBUTE_CONTRIBUTOR_KEY]
        val integrationId = request.pathVariable("id")
        return if (requestingContributor is SimpleContributor) {
            service.findSingleIntegration(integrationId, requestingContributor)?.let {
                val outputIntegration =
                    it.convertToDto(
                        requestingContributor as? SimpleContributor,
                        apiConfigs,
                        sourceConfigurations,
                        request,
                    )
                ok().contentType(MediaTypes.HAL_FORMS_JSON).bodyValueAndAwait(outputIntegration)
            } ?: resolveBadRequest("Non-existing Integration", "Integration")
        } else {
            resolveBadRequest("Invalid Contributor Token", "Contributor Token")
        }
    }

    /**
     * Handler for the Get All Integrations for a ProjectManagement endpoint,
     * even the ones that are not registered yet.
     *
     * @param request - HTTP `ServerRequest` object
     * @return the `ServerResponse`
     */
    suspend fun getProjectManagementIntegrations(request: ServerRequest): ServerResponse {
        val requestingContributor =
            request.attributes()[AngoraSixInfrastructure.REQUEST_ATTRIBUTE_CONTRIBUTOR_KEY]

        val projectManagementId = request.pathVariable("projectManagementId")
        return if (requestingContributor is SimpleContributor) {
            service.findIntegrationsForProjectManagement(projectManagementId, requestingContributor).map {
                it.convertToDto(
                    requestingContributor,
                    apiConfigs,
                    sourceConfigurations,
                    request,
                )
            }
                .let {
                    ok().contentType(MediaTypes.HAL_FORMS_JSON)
                        .bodyValueAndAwait(
                            it.convertToDto(
                                requestingContributor,
                                ListIntegrationFilter(),
                                apiConfigs,
                                request,
                            ),
                        )
                }
        } else {
            resolveBadRequest("Invalid Contributor Token", "Contributor Token")
        }
    }

    /**
     * Handler for the Create ProjectManagementIntegrations endpoint,
     * to create a new ProjectManagementIntegration entity.
     *
     * @param request - HTTP `ServerRequest` object
     * @return the `ServerResponse`
     */
    suspend fun registerIntegration(request: ServerRequest): ServerResponse {
        val requestingContributor =
            request.attributes()[AngoraSixInfrastructure.REQUEST_ATTRIBUTE_CONTRIBUTOR_KEY]
        val projectManagementId = request.pathVariable("projectManagementId")

        return if (requestingContributor is SimpleContributor) {
            val integration = try {
                request.awaitBody<IntegrationDto>()
                    .convertToDomain(
                        setOf(
                            SimpleContributor(
                                requestingContributor.contributorId,
                                emptySet(),
                            ),
                        ),
                        projectManagementId,
                    )
            } catch (e: IllegalArgumentException) {
                return resolveBadRequest(
                    e.message ?: "Incorrect Project Management body",
                    "Project Management",
                )
            }

            val outputIntegration = service.registerIntegration(integration, requestingContributor)
                .convertToDto(requestingContributor, apiConfigs, sourceConfigurations, request)

            val selfLink =
                outputIntegration.links.getRequiredLink(IanaLinkRelations.SELF).href

            created(URI.create(selfLink)).contentType(MediaTypes.HAL_FORMS_JSON)
                .bodyValueAndAwait(outputIntegration)
        } else {
            resolveBadRequest("Invalid Contributor Token", "Contributor Token")
        }
    }

//    /**
//     * Handler for the Update ProjectManagementIntegration endpoint,
//     * retrieving a Mono with the updated ProjectManagementIntegration.
//     *
//     * @param request - HTTP `ServerRequest` object
//     * @return the `ServerResponse`
//     */
//    suspend fun updateProjectManagementIntegration(request: ServerRequest): ServerResponse {
//        val requestingContributor =
//            request.attributes()[AngoraSixInfrastructure.REQUEST_ATTRIBUTE_CONTRIBUTOR_KEY]
//
//        return if (requestingContributor is SimpleContributor) {
//            val projectId = request.pathVariable("id")
//
//            val updateProjectManagementIntegrationData = try {
//                request.awaitBody<IntegrationDto>()
//                    .let { it.convertToDomain(it.admins ?: emptySet()) }
//            } catch (e: IllegalArgumentException) {
//                return resolveBadRequest(
//                    e.message ?: "Incorrect Project Management body",
//                    "Project Management",
//                )
//            }
//
//            service.updateIntegration(
//                projectId,
//                updateProjectManagementIntegrationData,
//                requestingContributor,
//            )?.let {
//                val outputProjectManagementIntegration =
//                    it.convertToDto(
//                        requestingContributor,
//                        apiConfigs,
//                        request,
//                    )
//
//                ok().contentType(MediaTypes.HAL_FORMS_JSON).bodyValueAndAwait(outputProjectManagementIntegration)
//            } ?: resolveNotFound("Can't update this project management", "Project Management")
//        } else {
//            resolveBadRequest("Invalid Contributor Token", "Contributor Token")
//        }
//    }
}

// private fun MultiValueMap<String, String>.toQueryFilter(): ListIntegrationFilter {
//    return ListIntegrationFilter(
//        get(ProjectsManagementIntegrationsQueryParams.PROJECT_MANAGEMENT_IDS.param)?.flatMap { it.split(",") },
//    )
// }
