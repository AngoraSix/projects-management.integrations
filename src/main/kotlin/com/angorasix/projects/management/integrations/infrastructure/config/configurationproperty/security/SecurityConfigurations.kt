package com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.security

import org.springframework.boot.context.properties.ConfigurationProperties

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
