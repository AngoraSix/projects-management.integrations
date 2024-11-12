package com.angorasix.projects.management.integrations.application

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.domain.projectmanagement.integrations.Source
import com.angorasix.projects.management.integrations.application.strategies.DataExchangeStrategy
import com.angorasix.projects.management.integrations.domain.integration.exchange.DataExchange
import com.angorasix.projects.management.integrations.domain.integration.exchange.DataExchangeStatusValues
import com.angorasix.projects.management.integrations.domain.integration.exchange.IntegrationAssetRepository
import com.angorasix.projects.management.integrations.domain.integration.exchange.modification.DataExchangeModification
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.ListDataExchangeFilter

/**
 *
 *
 * @author rozagerardo
 */
class DataExchangeService(
    private val repository: IntegrationAssetRepository,
    private val integrationsService: IntegrationsService,
    private val dataExchangeStrategies: Map<Source, DataExchangeStrategy>,
) {
    suspend fun createDataExchange(
        integrationId: String,
        requestingContributor: SimpleContributor,
    ): DataExchange? {
        val integration =
            integrationsService.findSingleIntegration(integrationId, requestingContributor)
        return integration?.let {
            val source = Source.valueOf(integration.source.uppercase())
            val dataExchange = dataExchangeStrategies[source]?.startDataExchange(
                integration,
                requestingContributor,
            )
            dataExchange?.let { repository.save(it) }
        }
    }

    /**
     * Method to modify [DataExchange].
     *
     */
    suspend fun modifyDataExchange(
        requestingContributor: SimpleContributor,
        dataExchangeId: String,
        modificationOperations: List<DataExchangeModification<out Any>>,
    ): DataExchange? {
        val persistedDataExchange = repository.findForContributorUsingFilter(
            ListDataExchangeFilter(listOf(dataExchangeId)),
            requestingContributor,
        )
        return persistedDataExchange?.let {
            val patchedDataExchange =
                modificationOperations.fold(it) { accumulatedDataExchange, op ->
                    op.modify(
                        requestingContributor,
                        accumulatedDataExchange,
                    )
                }
            val source = persistedDataExchange.source
            val dataExchangeStrategy = dataExchangeStrategies[source]
                ?: throw IllegalArgumentException("Source not supported for DataExchange operations: $source")
            val integration = integrationsService.findSingleIntegration(
                persistedDataExchange.integrationId,
                requestingContributor,
            )
                ?: throw IllegalArgumentException("Couldn't find associated integration [${persistedDataExchange.integrationId}] for dataExchange [$dataExchangeId] }")

            val updatedDataExchange = if (dataExchangeStrategy.isReadyForExchange(
                    patchedDataExchange,
                    integration,
                    requestingContributor,
                )
            ) {
                val assets = dataExchangeStrategy.exchangeData(
                    patchedDataExchange,
                    integration,
                    requestingContributor,
                )
                // persist assets (created)
                // send assets to message queue (esto con event / domain event)
                // update assets status (waiting sync) (esto con post-processing domain event)
                // Maybe as Domain Event / AOP?
                patchedDataExchange.status.status = DataExchangeStatusValues.COMPLETED
                patchedDataExchange
            } else {
                dataExchangeStrategy.processModification(
                    patchedDataExchange,
                    integration,
                    requestingContributor,
                )
            }
            repository.save(updatedDataExchange)
        }
    }
}
