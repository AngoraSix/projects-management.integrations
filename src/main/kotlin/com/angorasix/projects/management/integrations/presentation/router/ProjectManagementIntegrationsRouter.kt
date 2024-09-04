package com.angorasix.projects.management.integrations.presentation.router

import com.angorasix.commons.reactive.presentation.filter.extractRequestingContributor
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.api.ApiConfigs
import com.angorasix.projects.management.integrations.presentation.handler.ProjectManagementIntegrationsHandler
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
    fun projectRouterFunction() = coRouter {
        apiConfigs.basePaths.projectsManagementIntegration.nest {
            filter { request, next ->
                extractRequestingContributor(
                    request,
                    next,
                )
            }
            apiConfigs.routes.baseByProjectManagementIdCrudRoute.nest {
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
            apiConfigs.routes.baseByIdCrudRoute.nest {
                method(apiConfigs.routes.getIntegration.method).nest {
                    method(
                        apiConfigs.routes.getIntegration.method,
                        handler::getIntegration,
                    )
                }
            }
        }
    }
}
