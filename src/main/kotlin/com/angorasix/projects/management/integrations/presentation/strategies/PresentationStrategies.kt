package com.angorasix.projects.management.integrations.presentation.strategies

import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.api.ApiConfigs

abstract class SourceStrategy(
    val sourceConfigs: Map<String, String>,
) {
    abstract fun resolveRegistrationActions(apiConfigs: ApiConfigs): List<ActionData>
}

class TrelloStrategy(
    sourceConfigs: Map<String, String>,
) : SourceStrategy(sourceConfigs) {
    override fun resolveRegistrationActions(apiConfigs: ApiConfigs): List<ActionData> {
        val authUrlPattern =
            sourceConfigs["authorizationUrlPattern"]
                ?: throw IllegalArgumentException("authorizationUrl is required")

        val paramsRegex = """:(\w+)""".toRegex()

        val authUrl =
            paramsRegex.replace(authUrlPattern) { matchResult ->
                val key = matchResult.groupValues[1] // Get the key without the ":"
                sourceConfigs[key]
                    ?: matchResult.value // Replace with map value or leave unchanged if not found
            }
        return listOf(ActionData(apiConfigs.integrationActions.redirectAuthorization, authUrl))
    }
}

data class ActionData(
    val key: String,
    val url: String,
)
