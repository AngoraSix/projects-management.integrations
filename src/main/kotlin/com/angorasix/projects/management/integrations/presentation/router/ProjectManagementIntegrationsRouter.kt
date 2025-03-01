package com.angorasix.projects.management.integrations.presentation.router

import com.angorasix.commons.reactive.presentation.filter.extractRequestingContributor
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.api.ApiConfigs
import com.angorasix.projects.management.integrations.presentation.handler.ProjectManagementIntegrationsHandler
import com.angorasix.projects.management.integrations.presentation.handler.SourceSyncHandler
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
    private val sourceSyncHandler: SourceSyncHandler,
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
                apiConfigs.routes.baseByProjectManagementIdCrudRoute.nest {
                    defineByProjectManagementIdRoutes()
                }
                apiConfigs.routes.baseSourceSyncByIdCrudRoute.nest {
                    defineSourceSyncByIdEndpoints()
                }
                apiConfigs.routes.baseSourceSyncByIntegrationIdCrudRoute.nest {
                    defineSourceSyncBaseEndpoints()
                }
                apiConfigs.routes.baseByIdCrudRoute.nest {
                    defineByIdEndpoints()
                }
            }
        }

    private fun CoRouterFunctionDsl.defineByIdEndpoints() {
        method(apiConfigs.routes.getIntegration.method).nest {
            method(
                apiConfigs.routes.getIntegration.method,
                handler::getIntegration,
            )
        }
        method(apiConfigs.routes.patchIntegration.method).nest {
            method(
                apiConfigs.routes.patchIntegration.method,
                handler::patchIntegration,
            )
        }
    }

    private fun CoRouterFunctionDsl.defineSourceSyncBaseEndpoints() {
        method(apiConfigs.routes.createSourceSync.method).nest {
            method(
                apiConfigs.routes.createSourceSync.method,
                sourceSyncHandler::createSourceSync,
            )
        }
    }

    private fun CoRouterFunctionDsl.defineSourceSyncByIdEndpoints() {
        method(apiConfigs.routes.getSourceSync.method).nest {
            method(
                apiConfigs.routes.getSourceSync.method,
                sourceSyncHandler::getSourceSync,
            )
        }
        method(apiConfigs.routes.patchSourceSync.method).nest {
            method(
                apiConfigs.routes.patchSourceSync.method,
                sourceSyncHandler::patchSourceSync,
            )
        }
    }

    private fun CoRouterFunctionDsl.defineByProjectManagementIdRoutes() {
        method(apiConfigs.routes.listIntegrationsByProjectManagementId.method).nest {
            method(
                apiConfigs.routes.listIntegrationsByProjectManagementId.method,
                handler::getProjectManagementIntegrations,
            )
        }
        method(apiConfigs.routes.registerIntegrationForProjectManagement.method).nest {
            method(
                apiConfigs.routes.registerIntegrationForProjectManagement.method,
                handler::registerIntegration,
            )
        }
    }
}
