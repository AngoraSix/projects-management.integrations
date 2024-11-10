package com.angorasix.projects.management.integrations.presentation.handler

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.infrastructure.constants.AngoraSixInfrastructure
import com.angorasix.commons.reactive.presentation.error.resolveBadRequest
import com.angorasix.commons.reactive.presentation.error.resolveNotFound
import com.angorasix.projects.management.integrations.application.DataExchangeService
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.api.ApiConfigs
import org.springframework.hateoas.IanaLinkRelations
import org.springframework.hateoas.MediaTypes
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.created
import org.springframework.web.reactive.function.server.ServerResponse.ok
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
) {
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
//        val integrationId = request.pathVariable("integrationId")
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
//        val integrationId = request.pathVariable("integrationId")
//        val dataExchangeId = request.pathVariable("id")

        return if (requestingContributor is SimpleContributor) {
            // Implement the service method to patch the DataExchange
            ok().contentType(MediaTypes.HAL_FORMS_JSON).buildAndAwait()
        } else {
            resolveBadRequest("Invalid Contributor Token", "Contributor Token")
        }
    }
}
