package com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integration

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

/**
 * <p>
 *  Base file containing all Service configurations.
 * </p>
 *
 * @author rozagerardo
 */
@ConfigurationProperties(prefix = "configs.source")
data class SourceConfigurations(

    @NestedConfigurationProperty
    var trello: TrelloConfigs,
    var supported: Set<String>,
)

data class TrelloConfigs(
    val apiKey: String,
    val apiSecret: String,
)
