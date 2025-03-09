package com.angorasix.projects.management.integrations.presentation.mappings

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.infrastructure.config.configurationproperty.api.Route
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSync
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.api.ApiConfigs
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.SourceConfigurations
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.SourceSyncFilter
import com.angorasix.projects.management.integrations.presentation.dto.SourceSyncDto
import com.angorasix.projects.management.integrations.presentation.dto.SourceSyncMappingsUsersInputCollectionModel
import com.angorasix.projects.management.integrations.presentation.utils.uriBuilder
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.Link
import org.springframework.hateoas.mediatype.Affordances
import org.springframework.hateoas.server.core.EmbeddedWrapper
import org.springframework.hateoas.server.core.EmbeddedWrappers
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.server.ServerRequest

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
    sourceConfigurations: SourceConfigurations,
    request: ServerRequest,
): SourceSyncDto {
    val getSingleRoute = apiConfigs.routes.getSourceSync
    // self
    val selfLink =
        Link
            .of(uriBuilder(request).path(getSingleRoute.resolvePath()).build().toUriString())
            .withRel(getSingleRoute.name)
            .expand(id)
            .withSelfRel()
    val selfLinkWithDefaultAffordance =
        Affordances
            .of(selfLink)
            .afford(HttpMethod.OPTIONS)
            .withName("default")
            .toLink()
    add(selfLinkWithDefaultAffordance)

    requestingContributor?.let {
        if (requestingContributor.isAdminHint == true ||
            sourceSync.isAdmin(requestingContributor.contributorId)
        ) {
            addSourceSyncDtoAdminLinks(sourceSync, apiConfigs, sourceConfigurations, request)
        }
    }
    return this
}

private fun SourceSyncDto.addSourceSyncDtoAdminLinks(
    sourceSync: SourceSync,
    apiConfigs: ApiConfigs,
    sourceConfigurations: SourceConfigurations,
    request: ServerRequest,
) {
    if (sourceSync.isActive()) {
        // REQUEST FULL SYNC
        addLink(
            apiConfigs.routes.patchSourceSync,
            apiConfigs.integrationActions.requestFullSync,
            request,
        )

        // MATCH PLATFORM USERS
        addLink(
            apiConfigs.routes.getSourceSyncState,
            apiConfigs.integrationActions.getSourceSyncState,
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

        // DISABLE
        addLink(
            apiConfigs.routes.patchSourceSync,
            apiConfigs.integrationActions.disableIntegration,
            request,
        )
    } else {
        if (sourceSync.requiresFurtherConfiguration()) {
            // CONTINUE SYNC
            addLink(
                apiConfigs.routes.patchSourceSync,
                apiConfigs.integrationActions.continueSourceSync,
                request,
            )
        }

        // REGISTRATION ACTIONS (DEFINED BY SOURCE STRATEGY)
        sourceConfigurations.supported
            .flatMap {
                sourceConfigurations.sourceConfigs[it]?.resolvedStrategy?.resolveRegistrationActions(
                    apiConfigs,
                )
                    ?: emptyList()
            }.forEach { actionData ->
                val actionLink = Link.of(actionData.url).withRel(actionData.key)
                add(actionLink)
            }
    }
}

fun <T> List<T>.generateCollectionModel(clazz: Class<T>): Pair<Boolean, CollectionModel<T>> {
    val collectionModel =
        if (this.isEmpty()) {
            val wrappers = EmbeddedWrappers(false)
            val wrapper: EmbeddedWrapper = wrappers.emptyCollectionOf(clazz)
            CollectionModel.of(listOf(wrapper)) as CollectionModel<T>
        } else {
            CollectionModel.of(this).withFallbackType(clazz)
        }
    return Pair(this.isEmpty(), collectionModel)
}

fun CollectionModel<SourceSyncDto>.resolveHypermedia(
    requestingContributor: SimpleContributor?,
    filter: SourceSyncFilter,
    apiConfigs: ApiConfigs,
    request: ServerRequest,
): CollectionModel<SourceSyncDto> {
    val getByProjectManagementId = apiConfigs.routes.listSourceSyncsByProjectManagementId
    // self
    val selfLink =
        Link
            .of(
                uriBuilder(request)
                    .path(getByProjectManagementId.resolvePath())
                    .queryParams(filter.toMultiValueMap())
                    .build()
                    .toUriString(),
            ).withSelfRel()
    val selfLinkWithDefaultAffordance =
        Affordances
            .of(selfLink)
            .afford(HttpMethod.OPTIONS)
            .withName("default")
            .toLink()
    add(selfLinkWithDefaultAffordance)
    if (requestingContributor != null && requestingContributor.isAdminHint == true) {
        // here goes admin-specific collection hypermedia
    }
    return this
}

fun SourceSyncMappingsUsersInputCollectionModel.resolveHypermedia(
    requestingContributor: SimpleContributor?,
    apiConfigs: ApiConfigs,
    request: ServerRequest,
): SourceSyncMappingsUsersInputCollectionModel {
    val getByProjectManagementId = apiConfigs.routes.listSourceSyncsByProjectManagementId
    // self
    val selfLink =
        Link
            .of(
                uriBuilder(request)
                    .path(getByProjectManagementId.resolvePath())
                    .build()
                    .toUriString(),
            ).withSelfRel()
    val selfLinkWithDefaultAffordance =
        Affordances
            .of(selfLink)
            .afford(HttpMethod.OPTIONS)
            .withName("default")
            .toLink()
    add(selfLinkWithDefaultAffordance)
    if (requestingContributor != null && requestingContributor.isAdminHint == true) {
        // here goes admin-specific collection hypermedia
    }
    return this
}

private fun SourceSyncDto.addLink(
    route: Route,
    actionName: String,
    request: ServerRequest,
) {
    val actionLink =
        Link
            .of(
                uriBuilder(request)
                    .path(route.resolvePath())
                    .build()
                    .toUriString(),
            ).withTitle(actionName)
            .withName(actionName)
            .withRel(actionName)
    val affordanceLink =
        Affordances
            .of(actionLink)
            .afford(route.method)
            .withName(actionName)
            .toLink()
    add(affordanceLink)
}
