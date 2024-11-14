package com.angorasix.projects.management.integrations.presentation.handler

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.infrastructure.constants.AngoraSixInfrastructure
import com.angorasix.commons.presentation.dto.Patch
import com.angorasix.commons.reactive.presentation.error.resolveBadRequest
import com.angorasix.commons.reactive.presentation.error.resolveExceptionResponse
import com.angorasix.commons.reactive.presentation.error.resolveNotFound
import com.angorasix.projects.management.integrations.application.DataExchangeService
import com.angorasix.projects.management.integrations.domain.integration.exchange.modification.DataExchangeModification
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.api.ApiConfigs
import com.angorasix.projects.management.integrations.presentation.dto.SupportedDataExchangePatchOperations
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
import org.springframework.web.reactive.function.server.buildAndAwait
import java.net.URI

/**
 * ProjectManagementIntegration Handler (Controller) containing all handler functions
 * related to ProjectManagementIntegration endpoints.
 *
 * @author rozagerardo
 */
class DataExchangeHandler(
    private val service: DataExchangeService,
    private val apiConfigs: ApiConfigs,
    private val objectMapper: ObjectMapper,
) {
    /* default */
    val logger: Logger = LoggerFactory.getLogger(DataExchangeHandler::class.java)

    /**
     * Handler for the Create DataExchange endpoint for a particular Integration.
     *
     * @param request - HTTP `ServerRequest` object
     * @return the `ServerResponse`
     */
    suspend fun createDataExchange(request: ServerRequest): ServerResponse {
        val requestingContributor =
            request.attributes()[AngoraSixInfrastructure.REQUEST_ATTRIBUTE_CONTRIBUTOR_KEY]
        val integrationId = request.pathVariable("integrationId")

        return if (requestingContributor is SimpleContributor) {
            service.createDataExchange(integrationId, requestingContributor)
                ?.convertToDto(requestingContributor, apiConfigs, request)
                ?.let { outputDataExchangeDto ->
                    val selfLink =
                        outputDataExchangeDto?.links?.getRequiredLink(IanaLinkRelations.SELF)?.href

                    created(URI.create(selfLink)).contentType(MediaTypes.HAL_FORMS_JSON)
                        .bodyValueAndAwait(outputDataExchangeDto)
                } ?: resolveNotFound("Can't patch this Integration", "Integration")
        } else {
            resolveBadRequest("Invalid Contributor Token", "Contributor Token")
        }
    }

    /**
     * Handler for the Get DataExchange endpoint for a particular Integration.
     *
     * @param request - HTTP `ServerRequest` object
     * @return the `ServerResponse`
     */
    suspend fun getDataExchange(request: ServerRequest): ServerResponse {
        val requestingContributor =
            request.attributes()[AngoraSixInfrastructure.REQUEST_ATTRIBUTE_CONTRIBUTOR_KEY]
//        val dataExchangeId = request.pathVariable("id")

        return if (requestingContributor is SimpleContributor) {
            // Implement the service method to get the DataExchange
            ok().contentType(MediaTypes.HAL_FORMS_JSON).buildAndAwait()
        } else {
            resolveBadRequest("Invalid Contributor Token", "Contributor Token")
        }
    }

    /**
     * Handler for the Get DataExchange endpoint for a particular Integration.
     *
     * @param request - HTTP `ServerRequest` object
     * @return the `ServerResponse`
     */
    suspend fun patchDataExchange(request: ServerRequest): ServerResponse {
        val requestingContributor =
            request.attributes()[AngoraSixInfrastructure.REQUEST_ATTRIBUTE_CONTRIBUTOR_KEY]
        val dataExchangeId = request.pathVariable("id")
        val patch = request.awaitBody(Patch::class)

        return if (requestingContributor is SimpleContributor) {
            try {
                val modifyOperations = patch.operations.map {
                    it.toDomainObjectModification(
                        requestingContributor,
                        SupportedDataExchangePatchOperations.values().map { o -> o.op }.toList(),
                        objectMapper,
                    )
                }
                val modifyIntegrationOperations: List<DataExchangeModification<Any>> =
                    modifyOperations.filterIsInstance<DataExchangeModification<Any>>()
                val serviceOutput =
                    service.modifyDataExchange(
                        requestingContributor,
                        dataExchangeId,
                        modifyIntegrationOperations,
                    )
                serviceOutput?.convertToDto(
                    requestingContributor,
                    apiConfigs,
                    request,
                )?.let { ok().contentType(MediaTypes.HAL_FORMS_JSON).bodyValueAndAwait(it) }
                    ?: resolveNotFound("Can't patch this Data Exchange", "Data Exchange")
            } catch (ex: RuntimeException) {
                logger.error("Error while patching Data Exchange", ex)
                return resolveExceptionResponse(ex, "Data Exchange")
            }
        } else {
            resolveBadRequest("Invalid Contributor Token", "Contributor Token")
        }
    }
}
