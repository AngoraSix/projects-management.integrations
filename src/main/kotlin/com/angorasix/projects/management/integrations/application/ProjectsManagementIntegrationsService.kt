package com.angorasix.projects.management.integrations.application

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.domain.projectmanagement.integrations.Source
import com.angorasix.projects.management.integrations.application.strategies.RegistrationStrategy
import com.angorasix.projects.management.integrations.domain.integration.configuration.Integration
import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationConfig
import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationRepository
import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationStatus
import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationStatusValues
import com.angorasix.projects.management.integrations.domain.integration.configuration.modification.IntegrationModification
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.SourceConfigurations
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.ListIntegrationFilter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking

/**
 *
 *
 * @author rozagerardo
 */
class ProjectsManagementIntegrationsService(
    private val repository: IntegrationRepository,
    private val sourceConfigs: SourceConfigurations,
    private val registrationStrategies: Map<Source, RegistrationStrategy>,
) {
    suspend fun findSingleIntegration(
        id: String,
        requestingContributor: SimpleContributor,
    ): Integration? =
        repository.findForContributorUsingFilter(
            ListIntegrationFilter(listOf(id)),
            requestingContributor,
        )

    fun findIntegrationsForProjectManagement(
        projectManagementId: String,
        requestingContributor: SimpleContributor,
    ): List<Integration> = runBlocking {
        val filter = ListIntegrationFilter(null, null, listOf(projectManagementId))
        val integrationList = repository.findUsingFilter(filter, requestingContributor).toList()
        sourceConfigs.supported.map { source ->
            integrationList.find { it.source == source } ?: Integration(
                source,
                projectManagementId,
                IntegrationStatus(IntegrationStatusValues.NOT_REGISTERED),
                emptySet(),
                IntegrationConfig(null),
            )
        }
    }

    suspend fun registerIntegration(
        newIntegrationData: Integration,
        requestingContributor: SimpleContributor,
    ): Integration {
        val source = Source.valueOf(newIntegrationData.source.uppercase())
        val existingIntegration = repository.findForContributorUsingFilter(
            ListIntegrationFilter(
                null,
                setOf(source.value),
                listOf(newIntegrationData.projectManagementId),
            ),
            requestingContributor,
        )
        val processedRegisterIntegration =
            registrationStrategies[source]?.processIntegrationRegistration(
                newIntegrationData,
                requestingContributor,
                existingIntegration,
            ) ?: throw IllegalArgumentException("Source not supported")
        return repository.save(processedRegisterIntegration)
    }

    /**
     * Method to modify [Integration].
     *
     */
    suspend fun modifyIntegration(
        requestingContributor: SimpleContributor,
        integrationId: String,
        modificationOperations: List<IntegrationModification<out Any>>,
    ): Integration? {
        val integration = repository.findForContributorUsingFilter(
            ListIntegrationFilter(listOf(integrationId)),
            requestingContributor,
        )
        val updatedIntegration = integration?.let {
            modificationOperations.fold(it) { accumulatedIntegration, op ->
                op.modify(
                    requestingContributor,
                    accumulatedIntegration,
                )
            }
        }
        return updatedIntegration?.let { repository.save(it) }
    }
}
