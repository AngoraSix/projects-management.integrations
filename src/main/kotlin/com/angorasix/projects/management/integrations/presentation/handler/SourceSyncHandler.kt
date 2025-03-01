package com.angorasix.projects.management.integrations.presentation.handler

import com.angorasix.commons.domain.DetailedContributor
import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.infrastructure.constants.AngoraSixInfrastructure
import com.angorasix.commons.presentation.dto.Patch
import com.angorasix.commons.presentation.dto.convertToDto
import com.angorasix.commons.reactive.presentation.error.resolveBadRequest
import com.angorasix.commons.reactive.presentation.error.resolveExceptionResponse
import com.angorasix.commons.reactive.presentation.error.resolveNotFound
import com.angorasix.projects.management.integrations.application.SourceSyncService
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.modification.SourceSyncModification
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.api.ApiConfigs
import com.angorasix.projects.management.integrations.presentation.dto.ProjectContributorsToMatchDto
import com.angorasix.projects.management.integrations.presentation.dto.SupportedSourceSyncPatchOperations
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
class SourceSyncHandler(
    private val service: SourceSyncService,
    private val apiConfigs: ApiConfigs,
    private val objectMapper: ObjectMapper,
) {
    // default
    private val logger: Logger = LoggerFactory.getLogger(SourceSyncHandler::class.java)

    /**
     * Handler for the Create SourceSync endpoint for a particular Integration.
     *
     * @param request - HTTP `ServerRequest` object
     * @return the `ServerResponse`
     */
    suspend fun createSourceSync(request: ServerRequest): ServerResponse {
        val requestingContributor =
            request.attributes()[AngoraSixInfrastructure.REQUEST_ATTRIBUTE_CONTRIBUTOR_KEY]
        val integrationId = request.pathVariable("integrationId")

        return if (requestingContributor is SimpleContributor) {
            service
                .createSourceSync(integrationId, requestingContributor)
                ?.convertToDto(requestingContributor, apiConfigs, request, true)
                ?.let { outputSourceSyncDto ->
                    val selfLink =
                        outputSourceSyncDto.links.getRequiredLink(IanaLinkRelations.SELF).href
                    created(URI.create(selfLink))
                        .contentType(MediaTypes.HAL_FORMS_JSON)
                        .bodyValueAndAwait(outputSourceSyncDto)
                } ?: resolveNotFound(
                "Can't register Source Sync for this Integration / Source",
                "Sync Source",
            )
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
    suspend fun getSourceSync(request: ServerRequest): ServerResponse {
        val requestingContributor =
            request.attributes()[AngoraSixInfrastructure.REQUEST_ATTRIBUTE_CONTRIBUTOR_KEY]
        val sourceSyncId = request.pathVariable("id")

        return if (requestingContributor is SimpleContributor) {
            service.findSingleSourceSync(sourceSyncId, requestingContributor)?.let {
                val outputSourceSync =
                    it.convertToDto(
                        requestingContributor as? SimpleContributor,
                        apiConfigs,
                        request,
                        true,
                    )
                ok().contentType(MediaTypes.HAL_FORMS_JSON).bodyValueAndAwait(outputSourceSync)
            } ?: resolveBadRequest("Non-existing SourceSync", "SourceSync")
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

        return if (requestingContributor is DetailedContributor) {
            try {
                val modifyOperations =
                    patch.operations.map {
                        it.toDomainObjectModification(
                            requestingContributor,
                            SupportedSourceSyncPatchOperations.values().map { o -> o.op }.toList(),
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
                        request,
                        true, // println(update these with Trello-QhqSyHa7)
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
        return if (requestingContributor is SimpleContributor) {
            try {
                service
                    .startUserMatching(
                        projectContributorsDto.projectContributors,
                        sourceSyncId,
                        requestingContributor,
                    ).map {
                        it.convertToDto()
                    }.let {
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
