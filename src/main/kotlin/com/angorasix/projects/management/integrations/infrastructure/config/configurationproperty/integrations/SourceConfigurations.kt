package com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations

import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.strategies.SourceStrategy
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.strategies.TrelloStrategy
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

/**
 * <p>
 *  Base file containing all Service configurations.
 * </p>
 *
 * @author rozagerardo
 */
enum class SourceType(val key: String) {
    TRELLO("trello"),
}

@ConfigurationProperties(prefix = "configs.source")
class SourceConfigurations(


    sourceConfigs: Map<String, RawSourceConfiguration>,
    var supported: Set<String>,
) {
    @NestedConfigurationProperty
    var sourceConfigs: Map<String, SourceConfiguration> = sourceConfigs.map { (key, value) ->
        val strategy = when (key) {
            SourceType.TRELLO.key -> TrelloStrategy(value.strategyConfigs)
            else -> throw IllegalArgumentException("Unsupported source strategy: $key" +
                    "- should be one of: ${SourceType.values().joinToString { it.key }}")
        }
        key to SourceConfiguration(value.strategyConfigs, strategy)
    }.toMap()
}

open class RawSourceConfiguration(
    val strategyConfigs: Map<String, String>,
)

class SourceConfiguration(
    strategyConfigs: Map<String, String>,
    val resolvedStrategy: SourceStrategy,
): RawSourceConfiguration(strategyConfigs)