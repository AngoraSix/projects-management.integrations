package com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.api

import com.angorasix.commons.infrastructure.config.configurationproperty.api.Route
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

/**
 * <p>
 *  Base file containing all Service configurations.
 * </p>
 *
 * @author rozagerardo
 */
@ConfigurationProperties(prefix = "configs.api")
data class ApiConfigs(
    @NestedConfigurationProperty
    var routes: RoutesConfigs,
    @NestedConfigurationProperty
    var basePaths: BasePathConfigs,
    @NestedConfigurationProperty
    var integrationActions: IntegrationActions,
)

data class BasePathConfigs(
    val projectsManagementIntegration: String,
    val baseByIdCrudRoute: String,
    val baseByProjectManagementIdCrudRoute: String,
)

data class RoutesConfigs(
    val listSourceSyncsByProjectManagementId: Route,
    val registerSourceSyncForProjectManagement: Route,
    val getSourceSyncState: Route,
    val getSourceSync: Route,
    val patchSourceSync: Route,
    val startSourceSyncUsersMatch: Route,
)

data class IntegrationActions(
    val installInPlatform: String,
    val redirectAuthorization: String,
    val disableIntegration: String,
    val continueSourceSync: String,
    val requestFullSync: String,
    val updateSourceSyncConfig: String,
    val getSourceSyncState: String,
    val startMatchPlatformUsers: String,
)
