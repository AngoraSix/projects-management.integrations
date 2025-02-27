package com.angorasix.projects.management.integrations.application.strategies.source

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.domain.inputs.FieldSpec
import com.angorasix.commons.domain.inputs.InlineFieldSpec
import com.angorasix.commons.domain.inputs.OptionSpec
import com.angorasix.commons.domain.projectmanagement.integrations.Source
import com.angorasix.projects.management.integrations.application.strategies.SourceSyncStrategy
import com.angorasix.projects.management.integrations.application.strategies.typeReference
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAsset
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAssetStatus
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAssetSyncEvent
import com.angorasix.projects.management.integrations.domain.integration.asset.SourceAssetData
import com.angorasix.projects.management.integrations.domain.integration.asset.SourceAssetEstimationData
import com.angorasix.projects.management.integrations.domain.integration.configuration.Integration
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSync
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncEvent
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncEventValues
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncStatus
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncStatusStep
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncStatusValues
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceUser
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.SourceConfigurations
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.SourceType
import com.angorasix.projects.management.integrations.infrastructure.integrations.dto.TrelloBoardDto
import com.angorasix.projects.management.integrations.infrastructure.integrations.dto.TrelloCardDto
import com.angorasix.projects.management.integrations.infrastructure.integrations.dto.TrelloListDto
import com.angorasix.projects.management.integrations.infrastructure.integrations.dto.TrelloMemberDto
import com.angorasix.projects.management.integrations.infrastructure.integrations.dto.TrelloPluginDataA6ValueDto
import com.angorasix.projects.management.integrations.infrastructure.integrations.strategies.IntegrationConstants
import com.angorasix.projects.management.integrations.infrastructure.integrations.strategies.IntegrationConstants.Companion.ACCESS_TOKEN_CONFIG_PARAM
import com.angorasix.projects.management.integrations.infrastructure.security.TokenEncryptionUtil
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.bodyToFlow
import java.time.Instant
import java.time.format.DateTimeParseException

class TrelloSourceSyncStrategy(
    private val trelloWebClient: WebClient,
    private val integrationConfigs: SourceConfigurations,
    private val tokenEncryptionUtil: TokenEncryptionUtil,
    private val objectMapper: ObjectMapper,
) : SourceSyncStrategy {
    /* default */
    private val logger: Logger = LoggerFactory.getLogger(TrelloSourceSyncStrategy::class.java)

    override suspend fun configSourceSync(
        integration: Integration,
        requestingContributor: SimpleContributor,
        existingInProgressSourceSync: SourceSync?,
    ): SourceSync {
        return trelloStepsFns[stepKeysInOrder[0]]?.let {
            it(
                existingInProgressSourceSync,
                integration,
                requestingContributor,
            )
        }
            ?: throw IllegalArgumentException(
                "Trello Source Sync Strategy " +
                    "is not properly configured for configSourceSync",
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
        TrelloSteps.SELECT_BOARD to { existingInProgressSourceSync, integration, requestingContributor ->
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
                id = existingInProgressSourceSync?.id,
                source = Source.TRELLO.value,
                integrationId = integration.id
                    ?: throw IllegalArgumentException(
                        "persisted Integration" +
                            "is required for source sync",
                    ),
                status = SourceSyncStatus(
                    SourceSyncStatusValues.IN_PROGRESS,
                    arrayListOf(
                        SourceSyncStatusStep(
                            TrelloSteps.SELECT_BOARD.value,
                            listOf(boardFieldSpec),
                        ),
                    ),
                ),
                admins = setOf(requestingContributor),
                events = mutableListOf(
                    SourceSyncEvent(
                        SourceSyncEventValues.STARTING_FULL_SYNC_CONFIG,
                    ),
                ),
                sourceStrategyStateData = mapOf("boards" to boardsDto),
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
        syncEventId: String,
    ): List<IntegrationAsset> {
        val accessToken = extractAccessToken(integration)
        val boardCardsUrlPattern =
            integrationConfigs.sourceConfigs[SourceType.TRELLO.key]?.strategyConfigs
                ?.get("boardCardsUrlPattern")
                ?: throw IllegalArgumentException(
                    "trello boardCardsUrlPattern config" +
                        "is required for triggerSourceSync",
                )
        return obtainIntegrationAssets(
            sourceSync,
            integration,
            accessToken,
            boardCardsUrlPattern,
            syncEventId,
        )
    }

    @OptIn(FlowPreview::class)
    private suspend fun obtainIntegrationAssets(
        sourceSync: SourceSync,
        integration: Integration,
        accessToken: String,
        boardCardsUrlPattern: String,
        syncEventId: String,
    ): List<IntegrationAsset> {
        requireNotNull(integration.id) { "integration.id is required for triggerSourceSync" }
        requireNotNull(sourceSync.id) { "sourceSync.id is required for triggerSourceSync" }
        val trelloPluginId =
            integrationConfigs.sourceConfigs[SourceType.TRELLO.key]?.strategyConfigs?.get("pluginId")
        requireNotNull(trelloPluginId) { "trello pluginId config is required for triggerSourceSync" }
        val selectedBoardIds =
            sourceSync.status.steps.first { it.stepKey == TrelloSteps.SELECT_BOARD.value }
                .responseData?.get(TrelloResponseFieldKeys.SELECT_BOARD_FIELD.value)
        requireNotNull(selectedBoardIds) { "selected board is required for triggerSourceSync" }
        return selectedBoardIds.asFlow().flatMapMerge { selectedBoardId ->
            val doneListId =
                sourceSync.status.steps.first { it.stepKey == TrelloSteps.RESOLVE_BOARD.value }
                    .responseData?.get("${TrelloResponseFieldKeys.SELECT_DONE_LIST_PREFIX.value}$selectedBoardId")
                    ?.first()
            requireNotNull(doneListId) { "selected done list id is required for triggerSourceSync" }

            try {
                // Call Trello to get board cards List
                fetchAllCards(
                    trelloWebClient,
                    accessToken,
                    boardCardsUrlPattern,
                    selectedBoardId,
                    PAGING_LIMIT,
                ).map {
                    IntegrationAsset(
                        sourceSync.source,
                        integration.id,
                        sourceSync.id,
                        IntegrationAssetStatus(
                            mutableListOf(
                                IntegrationAssetSyncEvent.import(
                                    syncEventId,
                                ),
                            ),
                        ),
                        SourceAssetData(
                            id = it.id,
                            type = TrelloCardDto::class.java.name,
                            title = it.name,
                            description = it.desc,
                            dueInstant = parseDueDate(it.due),
                            assigneeIds = emptyList(),
                            done = it.idList == doneListId,
                            estimations = extractEstimationData(it, trelloPluginId),
                        ),
                        it,
                    )
                }
            } catch (e: WebClientResponseException) {
                throw IllegalArgumentException(
                    "Error while fetching cards for boardId: $selectedBoardId.",
                    e,
                )
            } catch (e: WebClientRequestException) {
                throw IllegalArgumentException(
                    "Error making cards request for boardId: $selectedBoardId.",
                    e,
                )
            }
        }.toList()
    }

    private fun extractEstimationData(
        it: TrelloCardDto,
        trelloPluginId: String,
    ): SourceAssetEstimationData? =
        it.pluginData
            ?.firstOrNull { pd -> pd.idPlugin == trelloPluginId }
            ?.value
            ?.takeIf { it.isNotBlank() }
            ?.let { json ->
                try {
                    val pluginValueDto = objectMapper.readValue(
                        json,
                        TrelloPluginDataA6ValueDto::class.java,
                    )
                    SourceAssetEstimationData(
                        caps = pluginValueDto.capsParams?.caps,
                        strategy = pluginValueDto.capsParams?.strategy,

                        effort = pluginValueDto.capsParams?.effort,
                        complexity = pluginValueDto.capsParams?.complexity,
                        industry = pluginValueDto.capsParams?.industry,
                        industryModifier = pluginValueDto.capsParams?.industryModifier,
                        moneyPayment = pluginValueDto.capsParams?.moneyPayment,
                    )
                } catch (ex: JsonProcessingException) {
                    logger.error("Error parsing plugin data for card: ${it.id}. Data: $json", ex)
                    null
                }
            }

    private fun extractAccessToken(integration: Integration) =
        tokenEncryptionUtil.decrypt(
            integration.config.sourceStrategyConfigData?.get(ACCESS_TOKEN_CONFIG_PARAM) as? String
                ?: throw IllegalArgumentException("trello access token body param is required for source sync"),
        )

    private fun parseDueDate(due: String?): Instant? {
        return try {
            due?.let { Instant.parse(due) }
        } catch (e: DateTimeParseException) {
            logger.error("Invalid due date format for Trello card: $due")
            null
        }
    }

    @OptIn(FlowPreview::class)
    override suspend fun obtainUsersMatchOptions(
        sourceSync: SourceSync,
        integration: Integration,
        requestingContributor: SimpleContributor,
    ): List<SourceUser> {
        val accessToken = extractAccessToken(integration)
        val boardMembersUrlPattern =
            integrationConfigs.sourceConfigs[SourceType.TRELLO.key]?.strategyConfigs
                ?.get("boardMembersUrlPattern")
                ?: throw IllegalArgumentException(
                    "trello boardCardsUrlPattern config" +
                        "is required for triggerSourceSync",
                )

        val selectedBoardIds =
            sourceSync.status.steps.first { it.stepKey == TrelloSteps.SELECT_BOARD.value }
                .responseData?.get(TrelloResponseFieldKeys.SELECT_BOARD_FIELD.value)
        requireNotNull(selectedBoardIds) { "selected board is required for triggerSourceSync" }

        return selectedBoardIds.asFlow().flatMapMerge { selectedBoardId ->
            try {
                // Call Trello to get board cards List
                fetchAllMembers(
                    trelloWebClient,
                    accessToken,
                    boardMembersUrlPattern,
                    selectedBoardId,
                ).map {
                    SourceUser(
                        sourceUserId = it.id,
                        name = it.fullName,
                        username = it.username,
                        email = it.email,
                        profileUrl = it.url,
                        profileMediaUrl = it.avatarUrl?.let { avatarUrlPattern -> "$avatarUrlPattern/50.png" },
                    )
                }
            } catch (e: WebClientResponseException) {
                throw IllegalArgumentException(
                    "Error while fetching members for boardId: $selectedBoardId.",
                    e,
                )
            } catch (e: WebClientRequestException) {
                throw IllegalArgumentException(
                    "Error making members request for boardId: $selectedBoardId.",
                    e,
                )
            }
        }.toList()
    }

    companion object {
        private const val PAGING_LIMIT = 1000
    }
}

suspend fun fetchAllCards(
    trelloWebClient: WebClient,
    accessToken: String,
    boardCardsUrlPattern: String,
    selectedBoardId: String,
    limit: Int,
): Flow<TrelloCardDto> {
    suspend fun fetchCards(since: String?): List<TrelloCardDto> {
        val boardCardsUrl =
            boardCardsUrlPattern.replace(":boardId", selectedBoardId)
                .replace(":limit", limit.toString())
                .replace(":since", since ?: "")
        return trelloWebClient.get()
            .uri(boardCardsUrl)
            .attributes { attrs ->
                attrs[IntegrationConstants.REQUEST_ATTRIBUTE_AUTHORIZATION_USER_TOKEN] =
                    accessToken
            }
            .retrieve().bodyToFlow<TrelloCardDto>().toList()
    }

    fun fetchPaginatedCards(since: String?): Flow<TrelloCardDto> = flow {
        var currentSince = since
        do {
            val cards = fetchCards(currentSince)
            cards.forEach { emit(it) }
            currentSince = cards.lastOrNull()?.id
        } while (cards.size == limit)
    }

    return fetchPaginatedCards(null)
}

fun fetchAllMembers(
    trelloWebClient: WebClient,
    accessToken: String,
    boardMembersUrlPattern: String,
    selectedBoardId: String,
): Flow<TrelloMemberDto> {
    val boardMembersUrl =
        boardMembersUrlPattern.replace(":boardId", selectedBoardId)
    return trelloWebClient.get()
        .uri(boardMembersUrl)
        .attributes { attrs ->
            attrs[IntegrationConstants.REQUEST_ATTRIBUTE_AUTHORIZATION_USER_TOKEN] =
                accessToken
        }
        .retrieve().bodyToFlow<TrelloMemberDto>()
}

enum class TrelloSteps(val value: String) {
    SELECT_BOARD("SELECT_BOARD"), RESOLVE_BOARD("RESOLVE_BOARD_MAPPING"),
}

private enum class TrelloResponseFieldKeys(val value: String) {
    SELECT_BOARD_FIELD("SELECT_BOARD_FIELD"), SELECT_DONE_LIST_PREFIX("SELECT_DONE_LIST_FIELD:"),
}
