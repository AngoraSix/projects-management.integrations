package com.angorasix.projects.management.integrations.presentation.router

import com.angorasix.commons.reactive.presentation.filter.extractRequestingContributor
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.api.ApiConfigs
import com.angorasix.projects.management.integrations.presentation.handler.ProjectManagementIntegrationsHandler
import org.springframework.web.reactive.function.server.CoRouterFunctionDsl
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.coRouter

/**
 * Router for all Project Management Integrations related endpoints.
 *
 * @author rozagerardo
 */
class ProjectManagementIntegrationsRouter(
    private val handler: ProjectManagementIntegrationsHandler,
    private val apiConfigs: ApiConfigs,
) {
    /**
     *
     * Main RouterFunction configuration for all endpoints related to ProjectManagements.
     *
     * @return the [RouterFunction] with all the routes for ProjectManagements
     */
    fun projectRouterFunction() =
        coRouter {
            apiConfigs.basePaths.projectsManagementIntegration.nest {
                filter { request, next ->
                    extractRequestingContributor(
                        request,
                        next,
                    )
                }
                apiConfigs.basePaths.baseByProjectManagementIdCrudRoute.nest {
                    defineByProjectManagementIdRoutes()
                }
                apiConfigs.basePaths.baseByIdCrudRoute.nest {
                    defineByIdEndpoints()
                }
            }
        }

    private fun CoRouterFunctionDsl.defineByIdEndpoints() {
        method(
            apiConfigs.routes.getSourceSync.method,
            handler::getSourceSync,
        )
        method(
            apiConfigs.routes.patchSourceSync.method,
            handler::patchSourceSync,
        )
        path(apiConfigs.routes.getSourceSyncState.path).nest {
            method(
                apiConfigs.routes.getSourceSyncState.method,
                handler::getSourceSyncState,
            )
        }
        path(apiConfigs.routes.startSourceSyncUsersMatch.path).nest {
            method(
                apiConfigs.routes.startSourceSyncUsersMatch.method,
                handler::startSourceSyncUsersMatch,
            )
        }
    }

    private fun CoRouterFunctionDsl.defineByProjectManagementIdRoutes() {
        method(
            apiConfigs.routes.listSourceSyncsByProjectManagementId.method,
            handler::getProjectManagementSourceSyncs,
        )
        method(
            apiConfigs.routes.registerSourceSyncForProjectManagement.method,
            handler::registerSourceSync,
        )
    }
}
