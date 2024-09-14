package com.angorasix.projects.management.integrations.presentation.handler

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.api.ApiConfigs
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.SourceConfigurations
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.ListIntegrationFilter
import com.angorasix.projects.management.integrations.presentation.dto.IntegrationDto
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.Link
import org.springframework.hateoas.mediatype.Affordances
import org.springframework.hateoas.server.core.EmbeddedWrapper
import org.springframework.hateoas.server.core.EmbeddedWrappers
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.util.UriComponentsBuilder

/**
 * <p>
 * </p>
 *
 * @author rozagerardo
 */
fun IntegrationDto.resolveHypermedia(
    requestingContributor: SimpleContributor?,
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
        if (requestingContributor.isAdminHint == true) {
            sourceConfigurations.supported.flatMap {
                sourceConfigurations.sourceConfigs[it]?.resolvedStrategy?.resolveRegistrationActions() ?: emptyList()
            }.forEach { actionData ->
                val actionLink = Link.of(actionData.value).withRel(actionData.key)
                add(actionLink)
            }
        }
    }
    return this
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

// fun CollectionModel<IntegrationDto>.resolveHypermedia(
//    requestingContributor: SimpleContributor?,
//    projectId: String,
//    apiConfigs: ApiConfigs,
//    request: ServerRequest,
//    isEmpty: Boolean,
// ): CollectionModel<IntegrationDto> {
//    val getByProjectManagementId = apiConfigs.routes.listIntegrationsByProjectManagementId
//    // self
//    val selfLink = Link.of(
//        uriBuilder(request).path(getByProjectManagementId.resolvePath()).build().toUriString(),
//    ).withRel(getByProjectManagementId.name).expand(projectId).withSelfRel()
//    val selfLinkWithDefaultAffordance =
//        Affordances.of(selfLink).afford(HttpMethod.OPTIONS).withName("default").toLink()
//    add(selfLinkWithDefaultAffordance)
//    return this
// }

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
        // here goes admin-specific hypermedia
    }
    return this
}

private fun uriBuilder(request: ServerRequest) = request.requestPath().contextPath().let {
    UriComponentsBuilder.fromHttpRequest(request.exchange().request).replacePath(it.toString()) //
        .replaceQuery("")
}
