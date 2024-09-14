package com.angorasix.projects.management.integrations.application

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.domain.projectmanagement.integrations.Source
import com.angorasix.projects.management.integrations.application.strategies.RegistrationStrategy
import com.angorasix.projects.management.integrations.domain.integration.configuration.Integration
import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationConfig
import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationRepository
import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationStatus
import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationStatusValues
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
    suspend fun findSingleIntegration(id: String, requestingContributor: SimpleContributor): Integration? =
        repository.findByIdForContributor(id, requestingContributor)
//
//    suspend fun findIntegrationByKey(source: String, projectManagementId: String): IntegrationAsset? =
//        repository.findBySourceAndProjectManagementId(source, projectManagementId)
//
//    fun findIntegrations(filter: ListIntegrationFilter): Flow<IntegrationAsset> =
//        repository.findUsingFilter(filter)

    fun findIntegrationsForProjectManagement(
        projectManagementId: String,
        requestingContributor: SimpleContributor,
    ): List<Integration> = runBlocking {
        val filter = ListIntegrationFilter(
            listOf(projectManagementId),
        )
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
        val processedRegisterIntegration =
            registrationStrategies[Source.valueOf(newIntegrationData.source.uppercase())]?.processIntegration(
                newIntegrationData,
                requestingContributor,
            ) ?: throw IllegalArgumentException("Source not supported")
        return repository.save(processedRegisterIntegration)
    }

//    suspend fun updateIntegration(
//        id: String,
//        updateData: Integration,
//        requestingContributor: SimpleContributor,
//    ): Integration? {
//        val projectManagementIntegrationToUpdate = repository.findByIdForContributor(
//            ListIntegrationFilter(
//                listOf(updateData.projectManagementId),
//                setOf(requestingContributor.contributorId),
//                listOf(id),
//            ),
//            requestingContributor,
//        )
//
//        return projectManagementIntegrationToUpdate?.updateWithData(updateData)?.let { repository.save(it) }
//    }

    /**
     * If a Integration exists, we update certain fields. If it doesn't we create it.
     * Integrations that are not included are ignored, at least for the moment.
     */
//    suspend fun projectManagementIntegrationsBatchUpdate(
//        projectManagementId: String,
//        source: Source,
//        adminContributor: SimpleContributor,
//        updatedIntegrations: List<IntegrationAsset>,
//    ): BulkResult {
//        // get the integration ids (as an alternative if that is required,
//        // if the flexible sourceStrategyConfigData doesn't work out)
// //        val sourceIntegrations = repository.findBySourceAndProjectManagementId(source.value, projectManagementId)
// //        val populatedIntegrations = updatedIntegrations.map {
// //            Integration(
// //                sourceIntegrations.find
// //                   { source -> source.integrationSourceId == it.sourceIntegrationId }?.integrationId,
// //                projectManagementId,
// //                   setOf(adminContributor), it.assignees, it.title, it.description, it.estimation,
// //            )
// //        }
// //        return repository.updateOrCreate(populatedIntegrations)
//        val sourceIntegrations = repository.findBySourceAndProjectManagementId(source.value, projectManagementId)
//        val populatedIntegrations = updatedIntegrations.map {
//            IntegrationAsset(
//                sourceIntegrations.find
//                  { source -> source.integrationSourceId == it.sourceIntegrationId }?.integrationId,
//                projectManagementId, setOf(adminContributor), it.assignees, it.title, it.description, it.estimation,
//            )
//        }
//        return repository.updateOrCreate(populatedIntegrations)
//    }
}
