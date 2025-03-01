package com.angorasix.projects.management.integrations.presentation.utils

import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.util.UriComponentsBuilder

fun uriBuilder(request: ServerRequest) =
    request.requestPath().contextPath().let {
        UriComponentsBuilder
            .fromHttpRequest(request.exchange().request)
            .replacePath(it.toString()) //
            .replaceQuery("")
    }
