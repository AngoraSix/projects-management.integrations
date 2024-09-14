package com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.strategies

import com.angorasix.projects.management.integrations.infrastructure.integrations.dto.ActionData

class SourceStrategiesConstants private constructor() {
    companion object {
        const val REDIRECT_AUTHORIZATION_STRATEGY_KEY = "redirectAuthorization"
    }
}

abstract class SourceStrategy(val sourceConfigs: Map<String, String>){
    abstract fun resolveRegistrationActions(): List<ActionData>
}

class TrelloStrategy(sourceConfigs: Map<String, String>): SourceStrategy(sourceConfigs) {
    override fun resolveRegistrationActions(): List<ActionData> {
        val authUrlPattern =
            sourceConfigs["authorizationUrlPattern"] ?: throw IllegalArgumentException("redirectAuthUrl is required")
        val appName = sourceConfigs["appName"] ?: throw IllegalArgumentException("appName is required")
        val apiKey = sourceConfigs["apiKey"] ?: throw IllegalArgumentException("apiKey is required")
        val authUrl = authUrlPattern.replace(":appName", appName).replace(":apiKey", apiKey)
        return listOf(ActionData(SourceStrategiesConstants.REDIRECT_AUTHORIZATION_STRATEGY_KEY, authUrl))
    }
}