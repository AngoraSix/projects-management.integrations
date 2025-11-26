package com.angorasix.projects.management.integrations.presentation.handler

import com.angorasix.commons.domain.A6Contributor
import com.angorasix.commons.infrastructure.constants.AngoraSixInfrastructure
import com.angorasix.commons.presentation.dto.Patch
import com.angorasix.commons.reactive.presentation.error.resolveBadRequest
import com.angorasix.commons.reactive.presentation.error.resolveExceptionResponse
import com.angorasix.commons.reactive.presentation.error.resolveNotFound
import com.angorasix.projects.management.integrations.application.SourceSyncService
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.modification.SourceSyncModification
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.api.ApiConfigs
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.SourceConfigurations
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.SourceSyncFilter
import com.angorasix.projects.management.integrations.presentation.dto.ProjectContributorsToMatchDto
import com.angorasix.projects.management.integrations.presentation.dto.SourceSyncDto
import com.angorasix.projects.management.integrations.presentation.dto.SupportedSourceSyncPatchOperations
import com.angorasix.projects.management.integrations.presentation.mappings.convertToDomain
import com.angorasix.projects.management.integrations.presentation.mappings.convertToDto
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
    private val service: SourceSyncService,
    private val apiConfigs: ApiConfigs,
    private val sourceConfigurations: SourceConfigurations,
    private val objectMapper: ObjectMapper,
) {
    // default
    private val logger: Logger = LoggerFactory.getLogger(ProjectManagementIntegrationsHandler::class.java)

    /**
     * Handler for the Get SourceSync endpoint for a particular Integration.
     *
     * @param request - HTTP `ServerRequest` object
     * @return the `ServerResponse`
     */
    suspend fun getSourceSync(request: ServerRequest): ServerResponse {
        val requestingContributor =
            request.attributes()[AngoraSixInfrastructure.REQUEST_ATTRIBUTE_CONTRIBUTOR_KEY]
        val sourceSyncId = request.pathVariable("id")

        return if (requestingContributor is A6Contributor) {
            service.findSingleSourceSync(sourceSyncId, requestingContributor)?.let {
                val outputSourceSync =
                    it.convertToDto(
                        requestingContributor,
                        apiConfigs,
                        sourceConfigurations,
                        request,
                    )
                ok().contentType(MediaTypes.HAL_FORMS_JSON).bodyValueAndAwait(outputSourceSync)
            } ?: resolveNotFound("Non-existing SourceSync", "SourceSync")
        } else {
            resolveBadRequest("Invalid Contributor Token", "Contributor Token")
        }
    }

    /**
     * Handler for the Get SourceSync endpoint for a particular Integration.
     *
     * @param request - HTTP `ServerRequest` object
     * @return the `ServerResponse`
     */
    suspend fun getSourceSyncState(request: ServerRequest): ServerResponse {
        val requestingContributor =
            request.attributes()[AngoraSixInfrastructure.REQUEST_ATTRIBUTE_CONTRIBUTOR_KEY]
        val sourceSyncId = request.pathVariable("id")

        return if (requestingContributor is A6Contributor) {
            service.findSingleSourceSync(sourceSyncId, requestingContributor)?.let {
                val outputSourceSync =
                    it.convertToDto(
                        requestingContributor,
                        apiConfigs,
                        sourceConfigurations,
                        request,
                    )
                ok().contentType(MediaTypes.HAL_FORMS_JSON).bodyValueAndAwait(outputSourceSync)
            } ?: resolveNotFound("Non-existing SourceSync", "SourceSync")
        } else {
            resolveBadRequest("Invalid Contributor Token", "Contributor Token")
        }
    }

    /**
     * Handler for the Get All SourceSyncs for a ProjectManagement endpoint,
     * even the ones that are not registered yet.
     *
     * @param request - HTTP `ServerRequest` object
     * @return the `ServerResponse`
     */
    suspend fun getProjectManagementSourceSyncs(request: ServerRequest): ServerResponse {
        val requestingContributor =
            request.attributes()[AngoraSixInfrastructure.REQUEST_ATTRIBUTE_CONTRIBUTOR_KEY]

        val projectManagementId = request.pathVariable("projectManagementId")
        return if (requestingContributor is A6Contributor) {
            service
                .findSourceSyncsForProjectManagement(projectManagementId, requestingContributor)
                .map {
                    it.convertToDto(
                        requestingContributor,
                        apiConfigs,
                        sourceConfigurations,
                        request,
                    )
                }.let {
                    ok()
                        .contentType(MediaTypes.HAL_FORMS_JSON)
                        .bodyValueAndAwait(
                            it.convertToDto(
                                SourceSyncFilter(),
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
    suspend fun registerSourceSync(request: ServerRequest): ServerResponse {
        val requestingContributor =
            request.attributes()[AngoraSixInfrastructure.REQUEST_ATTRIBUTE_CONTRIBUTOR_KEY]
        val projectManagementId = request.pathVariable("projectManagementId")

        return if (requestingContributor is A6Contributor) {
            try {
                request
                    .awaitBody<SourceSyncDto>()
                    .convertToDomain(projectManagementId)
                    .let { service.registerSourceSync(it, requestingContributor) }
                    .convertToDto(requestingContributor, apiConfigs, sourceConfigurations, request)
                    .let { outputSourceSyncDto ->
                        val selfLink =
                            outputSourceSyncDto.links.getRequiredLink(IanaLinkRelations.SELF).href
                        created(URI.create(selfLink))
                            .contentType(MediaTypes.HAL_FORMS_JSON)
                            .bodyValueAndAwait(outputSourceSyncDto)
                    }
            } catch (e: IllegalArgumentException) {
                return resolveBadRequest(
                    e.message ?: "Incorrect Source Sync body",
                    "Source Sync",
                )
            }
        } else {
            resolveBadRequest("Invalid Contributor Token", "Contributor Token")
        }
    }

    /**
     * Handler for the Patch SourceSync endpoint.
     *
     * @param request - HTTP `ServerRequest` object
     * @return the `ServerResponse`
     */
    suspend fun patchSourceSync(request: ServerRequest): ServerResponse {
        val requestingContributor =
            request.attributes()[AngoraSixInfrastructure.REQUEST_ATTRIBUTE_CONTRIBUTOR_KEY]
        val sourceSyncId = request.pathVariable("id")
        val patch = request.awaitBody(Patch::class)

        return if (requestingContributor is A6Contributor) {
            try {
                val modifyOperations =
                    patch.operations.map {
                        it.toDomainObjectModification(
                            requestingContributor,
                            SupportedSourceSyncPatchOperations.entries.map { o -> o.op }.toList(),
                            objectMapper,
                        )
                    }
                val modifyIntegrationOperations: List<SourceSyncModification<Any>> =
                    modifyOperations.filterIsInstance<SourceSyncModification<Any>>()
                val serviceOutput =
                    service.modifySourceSync(
                        requestingContributor,
                        sourceSyncId,
                        modifyIntegrationOperations,
                    )
                serviceOutput
                    ?.convertToDto(
                        requestingContributor,
                        apiConfigs,
                        sourceConfigurations,
                        request,
                    )?.let { ok().contentType(MediaTypes.HAL_FORMS_JSON).bodyValueAndAwait(it) }
                    ?: resolveNotFound("Can't patch this Source Sync", "Source Sync")
            } catch (ex: RuntimeException) {
                logger.error("Error while patching Source Sync", ex)
                return resolveExceptionResponse(ex, "Source Sync")
            }
        } else {
            resolveBadRequest("Invalid Contributor Token", "Contributor Token")
        }
    }

    /**
     * Handler to start a SourceSync users match process,
     * based on the received list of contributors.
     *
     * @param request - HTTP `ServerRequest` object
     * @return the `ServerResponse`
     */
    suspend fun startSourceSyncUsersMatch(request: ServerRequest): ServerResponse {
        val requestingContributor =
            request.attributes()[AngoraSixInfrastructure.REQUEST_ATTRIBUTE_CONTRIBUTOR_KEY]
        val sourceSyncId = request.pathVariable("id")
        val projectContributorsDto = request.awaitBody(ProjectContributorsToMatchDto::class)
        return if (requestingContributor is A6Contributor) {
            try {
                service
                    .startUserMatching(
                        projectContributorsDto.projectContributors,
                        sourceSyncId,
                        requestingContributor,
                    ).convertToDto(
                        requestingContributor,
                        apiConfigs,
                        request,
                    ).let {
                        ok().contentType(MediaTypes.HAL_FORMS_JSON).bodyValueAndAwait(it)
                    }
            } catch (ex: RuntimeException) {
                logger.error("Error while starting Source Sync Users Match process", ex)
                return resolveExceptionResponse(ex, "Source Sync Users Match")
            }
        } else {
            resolveBadRequest("Invalid Contributor Token", "Contributor Token")
        }
    }
}
