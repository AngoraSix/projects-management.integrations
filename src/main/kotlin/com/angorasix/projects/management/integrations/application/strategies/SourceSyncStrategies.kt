package com.angorasix.projects.management.integrations.application.strategies

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.domain.inputs.FieldSpec
import com.angorasix.commons.domain.inputs.InlineFieldSpec
import com.angorasix.commons.domain.inputs.OptionSpec
import com.angorasix.commons.domain.projectmanagement.integrations.Source
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAsset
import com.angorasix.projects.management.integrations.domain.integration.configuration.Integration
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSync
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncStatus
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncStatusStep
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncStatusValues
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.SourceConfigurations
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.SourceType
import com.angorasix.projects.management.integrations.infrastructure.integrations.dto.TrelloBoardDto
import com.angorasix.projects.management.integrations.infrastructure.integrations.dto.TrelloListDto
import com.angorasix.projects.management.integrations.infrastructure.integrations.strategies.IntegrationConstants
import com.angorasix.projects.management.integrations.infrastructure.integrations.strategies.IntegrationConstants.Companion.ACCESS_TOKEN_CONFIG_PARAM
import com.angorasix.projects.management.integrations.infrastructure.security.TokenEncryptionUtil
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Instant

inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

interface SourceSyncStrategy {
    suspend fun startSourceSync(
        integration: Integration,
        requestingContributor: SimpleContributor,
    ): SourceSync

    suspend fun isReadyForSyncing(
        sourceSync: SourceSync,
        integration: Integration,
        requestingContributor: SimpleContributor,
    ): Boolean

    suspend fun processModification(
        sourceSync: SourceSync,
        integration: Integration,
        requestingContributor: SimpleContributor,
    ): SourceSync

    suspend fun triggerSourceSync(
        sourceSync: SourceSync,
        integration: Integration,
        requestingContributor: SimpleContributor,
    ): List<IntegrationAsset>
}

class TrelloSourceSyncStrategy(
    private val trelloWebClient: WebClient,
    private val integrationConfigs: SourceConfigurations,
    private val tokenEncryptionUtil: TokenEncryptionUtil,
) : SourceSyncStrategy {
    override suspend fun startSourceSync(
        integration: Integration,
        requestingContributor: SimpleContributor,
    ): SourceSync {
        return trelloStepsFns[stepKeysInOrder[0]]?.let {
            it(
                null,
                integration,
                requestingContributor,
            )
        }
            ?: throw IllegalArgumentException(
                "Trello Source Sync Strategy " +
                    "is not properly configured for startSourceSync",
            )
    }

    override suspend fun isReadyForSyncing(
        sourceSync: SourceSync,
        integration: Integration,
        requestingContributor: SimpleContributor,
    ): Boolean {
        return sourceSync.status.steps.all { it.isCompleted() } &&
            sourceSync.status.steps.map { it.stepKey }
                .containsAll(TrelloSteps.values().map { it.value })
    }

    override suspend fun processModification(
        sourceSync: SourceSync,
        integration: Integration,
        requestingContributor: SimpleContributor,
    ): SourceSync {
        val currentStep = sourceSync.status.steps.size
        return trelloStepsFns[stepKeysInOrder[currentStep]]?.let {
            it(
                sourceSync,
                integration,
                requestingContributor,
            )
        }
            ?: throw IllegalArgumentException(
                "Trello Source Sync Strategy" +
                    "is not properly configured for step [$currentStep]",
            )
    }

    private val trelloStepsFns: Map<
        TrelloSteps,
        suspend (SourceSync?, Integration, SimpleContributor)
        -> SourceSync,
        > = mapOf(
        TrelloSteps.SELECT_BOARD to { _, integration, requestingContributor ->
            val accessToken = extractAccessToken(integration)
            val memberBoardsUri =
                integrationConfigs.sourceConfigs[SourceType.TRELLO.key]?.strategyConfigs
                    ?.get("memberBoardsUrl")
                    ?: throw IllegalArgumentException(
                        "trello memberBoardsUrl config" +
                            "is required for source sync",
                    )

            // Call Trello to get member Boards
            val boardsDto = trelloWebClient.get()
                .uri(memberBoardsUri)
                .attributes { attrs ->
                    attrs[IntegrationConstants.REQUEST_ATTRIBUTE_AUTHORIZATION_USER_TOKEN] =
                        accessToken
                }
                .retrieve().bodyToMono(typeReference<List<TrelloBoardDto>>()).awaitSingle()

            val boardsOptions = boardsDto.map { OptionSpec(it.id, it.name) }
            val boardFieldSpec =
                InlineFieldSpec(
                    TrelloResponseFieldKeys.SELECT_BOARD_FIELD.value,
                    FieldSpec.SELECT,
                    boardsOptions,
                )

            SourceSync(
                Source.TRELLO,
                integration.id
                    ?: throw IllegalArgumentException(
                        "persisted Integration" +
                            "is required for source sync",
                    ),
                Instant.now(),
                Instant.now(),
                SourceSyncStatus(
                    SourceSyncStatusValues.IN_PROGRESS,
                    arrayListOf(
                        SourceSyncStatusStep(
                            TrelloSteps.SELECT_BOARD.value,
                            listOf(boardFieldSpec),
                        ),
                    ),
                ),
                setOf(requestingContributor),
                mapOf("boards" to boardsDto),
            )
        },
        TrelloSteps.RESOLVE_BOARD to { sourceSync, integration, _ ->
            require(sourceSync != null) {
                "sourceSync" +
                    "is required for step ${TrelloSteps.RESOLVE_BOARD}"
            }

            val selectedBoardIds =
                sourceSync.status.steps.first { it.stepKey == TrelloSteps.SELECT_BOARD.value }
                    .responseData?.get(TrelloResponseFieldKeys.SELECT_BOARD_FIELD.value)
                    ?: throw IllegalArgumentException(
                        "selected board" +
                            "is required for ResolveBoard step",
                    )
            val boardListsUrlPattern =
                integrationConfigs.sourceConfigs[SourceType.TRELLO.key]?.strategyConfigs
                    ?.get("boardListsUrlPattern")
                    ?: throw IllegalArgumentException(
                        "trello boardListsUrlPattern config" +
                            "is required for source sync",
                    )
            val accessToken = extractAccessToken(integration)

            val fieldSpecs = coroutineScope {
                selectedBoardIds.map { selectedBoardId ->
                    async {
                        val boardListsUrl =
                            boardListsUrlPattern.replace(":boardId", selectedBoardId)
                        try {
                            // Call Trello to get board Lists
                            val listsDto = trelloWebClient.get()
                                .uri(boardListsUrl)
                                .attributes { attrs ->
                                    attrs[IntegrationConstants.REQUEST_ATTRIBUTE_AUTHORIZATION_USER_TOKEN] =
                                        accessToken
                                }
                                .retrieve().bodyToMono(typeReference<List<TrelloListDto>>())
                                .awaitSingle()
                            val doneListOptions = listsDto.map { OptionSpec(it.id, it.name) }
                            InlineFieldSpec(
                                "${TrelloResponseFieldKeys.SELECT_DONE_LIST_PREFIX.value}$selectedBoardId",
                                FieldSpec.SELECT,
                                doneListOptions,
                            )
                        } catch (e: WebClientResponseException) {
                            throw IllegalArgumentException(
                                "Error while fetching board lists for boardId: $selectedBoardId." +
                                    "Are you sure board has lists?",
                                e,
                            )
                        } catch (e: WebClientRequestException) {
                            throw IllegalArgumentException(
                                "Error making board lists request for boardId: $selectedBoardId." +
                                    "Are you sure board has lists?",
                                e,
                            )
                        }
                    }
                }
            }.awaitAll()

            val step = SourceSyncStatusStep(
                TrelloSteps.RESOLVE_BOARD.value,
                fieldSpecs,
            )

            sourceSync.status.steps.add(step)
            sourceSync
        },
    )

    private val stepKeysInOrder: List<TrelloSteps> = trelloStepsFns.keys.toList()

    override suspend fun triggerSourceSync(
        sourceSync: SourceSync,
        integration: Integration,
        requestingContributor: SimpleContributor,
    ): List<IntegrationAsset> {
        // "Not yet implemented")
        return emptyList()
    }

    private fun extractAccessToken(integration: Integration) =
        tokenEncryptionUtil.decrypt(
            integration.config.sourceStrategyConfigData?.get(ACCESS_TOKEN_CONFIG_PARAM) as? String
                ?: throw IllegalArgumentException("trello access token body param is required for source sync"),
        )
}

enum class TrelloSteps(val value: String) {
    SELECT_BOARD("SELECT_BOARD"), RESOLVE_BOARD("RESOLVE_BOARD_MAPPING"),
}

private enum class TrelloResponseFieldKeys(val value: String) {
    SELECT_BOARD_FIELD("SELECT_BOARD_FIELD"), SELECT_DONE_LIST_PREFIX("SELECT_DONE_LIST_FIELD:"),
}
