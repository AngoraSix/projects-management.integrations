//package com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integration.strategies
//
//import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integration.ActionData
//import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integration.StrategiesConfigurations
//import java.net.URL
//
//abstract class RegistrationStrategy(val key: String) {
//    abstract fun resolveActions(): List<ActionData>
//}
//
//abstract class RedirectAuthorizationStrategies(val url: URL) :
//    RegistrationStrategy(StrategiesConfigurations.REDIRECT_AUTHORIZATION_STRATEGY_KEY)