package com.angorasix.projects.management.integrations.presentation.handler

import IntegrationModification
import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.infrastructure.constants.AngoraSixInfrastructure
import com.angorasix.commons.presentation.dto.Patch
import com.angorasix.commons.reactive.presentation.error.resolveBadRequest
import com.angorasix.commons.reactive.presentation.error.resolveExceptionResponse
import com.angorasix.commons.reactive.presentation.error.resolveNotFound
import com.angorasix.commons.reactive.presentation.utils.affectedContributors
import com.angorasix.projects.management.integrations.application.ProjectsManagementIntegrationsService
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.api.ApiConfigs
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.SourceConfigurations
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.ListIntegrationFilter
import com.angorasix.projects.management.integrations.presentation.dto.IntegrationDto
import com.angorasix.projects.management.integrations.presentation.dto.SupportedPatchOperations
import com.fasterxml.jackson.databind.ObjectMapper
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
    private val objectMapper: ObjectMapper,
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
            service.findIntegrationsForProjectManagement(projectManagementId, requestingContributor)
                .map {
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

    /**
     * Handler for the Patch Club endpoint, retrieving a Mono with the requested Club.
     *
     * @param request - HTTP `ServerRequest` object
     * @return the `ServerResponse`
     */
    suspend fun patchIntegration(request: ServerRequest): ServerResponse {
        val contributor =
            request.attributes()[AngoraSixInfrastructure.REQUEST_ATTRIBUTE_CONTRIBUTOR_KEY]
        val integrationId = request.pathVariable("id")
        val patch = request.awaitBody(Patch::class)
        return if (contributor is SimpleContributor) {
            try {
                val modifyOperations = patch.operations.map {
                    it.toDomainObjectModification(
                        contributor,
                        SupportedPatchOperations.values().map { o -> o.op }.toList(),
                        objectMapper,
                    )
                }
                val modifyClubOperations: List<IntegrationModification<Any>> =
                    modifyOperations.filterIsInstance<IntegrationModification<Any>>()
                val serviceOutput =
                    service.modifyIntegration(contributor, integrationId, modifyClubOperations)
                serviceOutput?.convertToDto(
                    contributor,
                    apiConfigs,
                    sourceConfigurations,
                    request,
                )
                    ?.let { ok().contentType(MediaTypes.HAL_FORMS_JSON).bodyValueAndAwait(it)}
                    ?: resolveNotFound("Can't patch this Integration", "Integration")
            } catch (ex: RuntimeException) {
                return resolveExceptionResponse(ex, "Integration")
            }
        } else {
            resolveBadRequest("Invalid Contributor", "Contributor")
        }
    }
}
