package com.angorasix.projects.management.integrations.presentation.mappings

import com.angorasix.commons.domain.A6Contributor
import com.angorasix.commons.reactive.presentation.mappings.addLink
import com.angorasix.commons.reactive.presentation.mappings.addSelfLink
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSync
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.api.ApiConfigs
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.SourceConfigurations
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.SourceSyncFilter
import com.angorasix.projects.management.integrations.presentation.dto.SourceSyncDto
import com.angorasix.projects.management.integrations.presentation.dto.SourceSyncMappingsUsersInputCollectionModel
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.Link
import org.springframework.hateoas.server.core.EmbeddedWrapper
import org.springframework.hateoas.server.core.EmbeddedWrappers
import org.springframework.web.reactive.function.server.ServerRequest

/**
 * <p> Add HATEOAS links for Source Sync.
 * </p>
 *
 * @author rozagerardo
 */
fun SourceSyncDto.resolveHypermedia(
    requestingContributor: A6Contributor?,
    sourceSync: SourceSync,
    apiConfigs: ApiConfigs,
    sourceConfigurations: SourceConfigurations,
    request: ServerRequest,
): SourceSyncDto {
    // self
    addSelfLink(apiConfigs.routes.getSourceSync, request, listOf(id ?: "undefinedSourceSyncId"))

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
    val actionLink =
        Link
            .of(
                sourceConfigurations.extractSourceConfig(sourceSync.source, "installInPlatform"),
            ).withRel(apiConfigs.integrationActions.installInPlatform)
    add(actionLink)
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
    filter: SourceSyncFilter,
    apiConfigs: ApiConfigs,
    request: ServerRequest,
): CollectionModel<SourceSyncDto> {
    // self
    addSelfLink(
        apiConfigs.routes.listSourceSyncsByProjectManagementId,
        request,
        filter.projectManagementId?.let { listOf(it.first()) } ?: emptyList(),
    )
    // add admin-specific hypermedia here if requestingContributor.isAdminHint == true
    return this
}

fun SourceSyncMappingsUsersInputCollectionModel.resolveHypermedia(
    requestingContributor: A6Contributor?,
    apiConfigs: ApiConfigs,
    request: ServerRequest,
): SourceSyncMappingsUsersInputCollectionModel {
    addSelfLink(apiConfigs.routes.listSourceSyncsByProjectManagementId, request)
    if (requestingContributor != null && requestingContributor.isAdminHint == true) {
        // here goes admin-specific collection hypermedia
    }
    return this
}
