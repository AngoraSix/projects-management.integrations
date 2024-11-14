package com.angorasix.projects.management.integrations.presentation.handler

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.projects.management.integrations.domain.integration.configuration.Integration
import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationStatusValues
import com.angorasix.projects.management.integrations.domain.integration.exchange.DataExchange
import com.angorasix.projects.management.integrations.domain.integration.exchange.DataExchangeStatusValues
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.api.ApiConfigs
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.SourceConfigurations
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.ListIntegrationFilter
import com.angorasix.projects.management.integrations.presentation.dto.DataExchangeDto
import com.angorasix.projects.management.integrations.presentation.dto.IntegrationDto
import com.angorasix.projects.management.integrations.presentation.utils.uriBuilder
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.Link
import org.springframework.hateoas.mediatype.Affordances
import org.springframework.hateoas.server.core.EmbeddedWrapper
import org.springframework.hateoas.server.core.EmbeddedWrappers
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.server.ServerRequest

/**
 * <p>
 * </p>
 *
 * @author rozagerardo
 */
fun IntegrationDto.resolveHypermedia(
    requestingContributor: SimpleContributor?,
    integration: Integration,
    apiConfigs: ApiConfigs,
    sourceConfigurations: SourceConfigurations,
    request: ServerRequest,
): IntegrationDto {
    val getSingleRoute = apiConfigs.routes.getIntegration
    // self
    val selfLink =
        Link.of(uriBuilder(request).path(getSingleRoute.resolvePath()).build().toUriString())
            .withRel(getSingleRoute.name).expand(id).withSelfRel()
    val selfLinkWithDefaultAffordance =
        Affordances.of(selfLink).afford(HttpMethod.OPTIONS).withName("default").toLink()
    add(selfLinkWithDefaultAffordance)

    requestingContributor?.let {
        if (requestingContributor.isAdminHint == true || integration.isAdmin(requestingContributor.contributorId)) {
            addIntegrationDtoAdminLinks(integration, apiConfigs, sourceConfigurations, request)
        }
    }
    return this
}

private fun IntegrationDto.addIntegrationDtoAdminLinks(
    integration: Integration,
    apiConfigs: ApiConfigs,
    sourceConfigurations: SourceConfigurations,
    request: ServerRequest,
) {
    if (status?.status in listOf(
            IntegrationStatusValues.NOT_REGISTERED,
            IntegrationStatusValues.DISABLED,
        )
    ) {
        sourceConfigurations.supported.flatMap {
            sourceConfigurations.sourceConfigs[it]?.resolvedStrategy?.resolveRegistrationActions(
                apiConfigs,
            )
                ?: emptyList()
        }.forEach { actionData ->
            val actionLink = Link.of(actionData.url).withRel(actionData.key)
            add(actionLink)
        }
    } else {
        // IMPORT DATA
        val createDataExchangeRoute = apiConfigs.routes.createDataExchange
        val importDataActionName = apiConfigs.integrationActions.importData
        val createDataExchangeLink = Link.of(
            uriBuilder(request).path(createDataExchangeRoute.resolvePath()).build()
                .toUriString(),
        ).withTitle(importDataActionName).withName(importDataActionName)
            .withRel(importDataActionName).expand(integration.id)
        val createDataExchangeAffordanceLink =
            Affordances.of(createDataExchangeLink).afford(createDataExchangeRoute.method)
                .withName(importDataActionName).toLink()
        add(createDataExchangeAffordanceLink)

        // DISABLE
        val patchIntegrationRoute = apiConfigs.routes.patchIntegration
        val disableActionName = apiConfigs.integrationActions.disableIntegration
        val disableActionLink = Link.of(
            uriBuilder(request).path(patchIntegrationRoute.resolvePath()).build()
                .toUriString(),
        ).withTitle(disableActionName).withName(disableActionName)
            .withRel(disableActionName)
        val disableAffordanceLink =
            Affordances.of(disableActionLink).afford(patchIntegrationRoute.method)
                .withName(disableActionName).toLink()
        add(disableAffordanceLink)
    }
}

fun List<IntegrationDto>.generateCollectionModel(): Pair<Boolean, CollectionModel<IntegrationDto>> {
    val collectionModel = if (this.isEmpty()) {
        val wrappers = EmbeddedWrappers(false)
        val wrapper: EmbeddedWrapper = wrappers.emptyCollectionOf(IntegrationDto::class.java)
        CollectionModel.of(listOf(wrapper)) as CollectionModel<IntegrationDto>
    } else {
        CollectionModel.of(this).withFallbackType(IntegrationDto::class.java)
    }
    return Pair(this.isEmpty(), collectionModel)
}

fun CollectionModel<IntegrationDto>.resolveHypermedia(
    requestingContributor: SimpleContributor?,
    filter: ListIntegrationFilter,
    apiConfigs: ApiConfigs,
    request: ServerRequest,
): CollectionModel<IntegrationDto> {
    val getByProjectManagementId = apiConfigs.routes.listIntegrationsByProjectManagementId
    // self
    val selfLink = Link.of(
        uriBuilder(request).path(getByProjectManagementId.resolvePath())
            .queryParams(filter.toMultiValueMap()).build()
            .toUriString(),
    ).withSelfRel()
    val selfLinkWithDefaultAffordance =
        Affordances.of(selfLink).afford(HttpMethod.OPTIONS).withName("default").toLink()
    add(selfLinkWithDefaultAffordance)
    if (requestingContributor != null && requestingContributor.isAdminHint == true) {
        // here goes admin-specific collection hypermedia
    }
    return this
}

/**
 * <p> Add HATEOAS links for Data Exchange.
 * </p>
 *
 * @author rozagerardo
 */
fun DataExchangeDto.resolveHypermedia(
    requestingContributor: SimpleContributor?,
    dataExchange: DataExchange,
    apiConfigs: ApiConfigs,
    request: ServerRequest,
): DataExchangeDto {
    val getSingleRoute = apiConfigs.routes.getDataExchange
    // self
    val selfLink =
        Link.of(uriBuilder(request).path(getSingleRoute.resolvePath()).build().toUriString())
            .withRel(getSingleRoute.name).expand(dataExchange.integrationId, id).withSelfRel()
    val selfLinkWithDefaultAffordance =
        Affordances.of(selfLink).afford(HttpMethod.OPTIONS).withName("default").toLink()
    add(selfLinkWithDefaultAffordance)

    requestingContributor?.let {
        if (requestingContributor.isAdminHint == true || dataExchange.isAdmin(requestingContributor.contributorId)) {
            if (status?.status == DataExchangeStatusValues.IN_PROGRESS) {
                val patchDataExchangeRoute = apiConfigs.routes.patchDataExchange
                val continueDataExchangeActionName =
                    apiConfigs.integrationActions.continueDataExchange
                val continueDataExchangeActionLink = Link.of(
                    uriBuilder(request).path(patchDataExchangeRoute.resolvePath()).build()
                        .toUriString(),
                ).withTitle(continueDataExchangeActionName).withName(continueDataExchangeActionName)
                    .withRel(continueDataExchangeActionName)
                val continueDataExchangeAffordanceLink =
                    Affordances.of(continueDataExchangeActionLink)
                        .afford(patchDataExchangeRoute.method)
                        .withName(continueDataExchangeActionName).toLink()
                add(continueDataExchangeAffordanceLink)
            }
        }
    }
    return this
}
