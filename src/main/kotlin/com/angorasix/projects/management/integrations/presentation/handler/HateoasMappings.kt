package com.angorasix.projects.management.integrations.presentation.handler

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.infrastructure.config.configurationproperty.api.Route
import com.angorasix.projects.management.integrations.domain.integration.configuration.Integration
import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationStatusValues
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSync
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncStatusValues
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.api.ApiConfigs
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.SourceConfigurations
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.ListIntegrationFilter
import com.angorasix.projects.management.integrations.presentation.dto.IntegrationDto
import com.angorasix.projects.management.integrations.presentation.dto.SourceSyncDto
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
        // START SOURCE SYNC
        if (integration.sourceSync?.status?.status != SourceSyncStatusValues.COMPLETED) {
            val createSourceSyncRoute = apiConfigs.routes.createSourceSync
            val startConfigSourceSyncActionName =
                apiConfigs.integrationActions.startConfigSourceSync
            val createSourceSyncLink = Link.of(
                uriBuilder(request).path(createSourceSyncRoute.resolvePath()).build()
                    .toUriString(),
            ).withTitle(startConfigSourceSyncActionName).withName(startConfigSourceSyncActionName)
                .withRel(startConfigSourceSyncActionName).expand(integration.id)
            val createSourceSyncAffordanceLink =
                Affordances.of(createSourceSyncLink).afford(createSourceSyncRoute.method)
                    .withName(startConfigSourceSyncActionName).toLink()
            add(createSourceSyncAffordanceLink)
        }

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
 * <p> Add HATEOAS links for Source Sync.
 * </p>
 *
 * @author rozagerardo
 */
fun SourceSyncDto.resolveHypermedia(
    requestingContributor: SimpleContributor?,
    sourceSync: SourceSync,
    apiConfigs: ApiConfigs,
    request: ServerRequest,
    isIntegrationActive: Boolean,
): SourceSyncDto {
    val getSingleRoute = apiConfigs.routes.getSourceSync
    // self
    val selfLink =
        Link.of(uriBuilder(request).path(getSingleRoute.resolvePath()).build().toUriString())
            .withRel(getSingleRoute.name).expand(sourceSync.integrationId, id).withSelfRel()
    val selfLinkWithDefaultAffordance =
        Affordances.of(selfLink).afford(HttpMethod.OPTIONS).withName("default").toLink()
    add(selfLinkWithDefaultAffordance)

    requestingContributor?.let {
        if (
            (
                requestingContributor.isAdminHint == true ||
                    sourceSync.isAdmin(requestingContributor.contributorId)
                ) &&
            isIntegrationActive
        ) {
            if (status?.status == SourceSyncStatusValues.IN_PROGRESS) {
                // CONTINUE SYNC
                addLink(
                    apiConfigs.routes.patchSourceSync,
                    apiConfigs.integrationActions.continueSourceSync,
                    request,
                )
            } else if (status?.status == SourceSyncStatusValues.COMPLETED) {
                // REQUEST FULL SYNC
                addLink(
                    apiConfigs.routes.patchSourceSync,
                    apiConfigs.integrationActions.requestFullSync,
                    request,
                )

                // UPDATE SYNC CONFIG
                addLink(
                    apiConfigs.routes.patchSourceSync,
                    apiConfigs.integrationActions.updateSourceSyncConfig,
                    request,
                )

                // MATCH PLATFORM USERS
                addLink(
                    apiConfigs.routes.patchSourceSync,
                    apiConfigs.integrationActions.startMatchPlatformUsers,
                    request,
                )
            }
        }
    }
    return this
}

private fun SourceSyncDto.addLink(route: Route, actionName: String, request: ServerRequest) {
    val actionLink = Link.of(
        uriBuilder(request).path(route.resolvePath()).build()
            .toUriString(),
    ).withTitle(actionName).withName(actionName)
        .withRel(actionName)
    val affordanceLink =
        Affordances.of(actionLink)
            .afford(route.method)
            .withName(actionName).toLink()
    add(affordanceLink)
}
