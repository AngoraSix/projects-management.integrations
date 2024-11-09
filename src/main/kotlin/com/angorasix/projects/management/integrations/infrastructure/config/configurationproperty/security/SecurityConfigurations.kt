package com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.security

import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.strategies.SourceStrategy
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.strategies.TrelloStrategy
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

/**
 * <p>
 *  Base file containing all Service Security configurations.
 * </p>
 *
 * @author rozagerardo
 */

@ConfigurationProperties(prefix = "configs.security")
class SecurityConfigurations(
    var secretKey: String,
)