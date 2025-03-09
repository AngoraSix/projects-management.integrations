package com.angorasix.projects.management.integrations.application.strategies.source

import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.domain.inputs.FieldSpec
import com.angorasix.commons.domain.inputs.InlineFieldSpec
import com.angorasix.commons.domain.inputs.OptionSpec
import com.angorasix.projects.management.integrations.application.strategies.SourceSyncStrategy
import com.angorasix.projects.management.integrations.application.strategies.typeReference
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAsset
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAssetStatus
import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAssetSyncEvent
import com.angorasix.projects.management.integrations.domain.integration.asset.SourceAssetData
import com.angorasix.projects.management.integrations.domain.integration.asset.SourceAssetEstimationData
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSync
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncConfig
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncStatus
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceSyncStatusStep
import com.angorasix.projects.management.integrations.domain.integration.sourcesync.SourceUser
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.SourceConfigurations
import com.angorasix.projects.management.integrations.infrastructure.config.configurationproperty.integrations.SourceType
import com.angorasix.projects.management.integrations.infrastructure.integrations.dto.TrelloBoardDto
import com.angorasix.projects.management.integrations.infrastructure.integrations.dto.TrelloCardDto
import com.angorasix.projects.management.integrations.infrastructure.integrations.dto.TrelloMemberDto
import com.angorasix.projects.management.integrations.infrastructure.integrations.dto.TrelloPluginDataA6ValueDto
import com.angorasix.projects.management.integrations.infrastructure.integrations.strategies.IntegrationConstants
import com.angorasix.projects.management.integrations.infrastructure.security.TokenEncryptionUtil
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val sourceConfigs: SourceConfigurations,
    private val tokenEncryptionUtil: TokenEncryptionUtil,
    private val objectMapper: ObjectMapper,
) : SourceSyncStrategy {
    // default
    private val logger: Logger = LoggerFactory.getLogger(TrelloSourceSyncStrategy::class.java)

    override suspend fun resolveSourceSyncRegistration(
        sourceSyncData: SourceSync,
        requestingContributor: SimpleContributor,
        existingSourceSync: SourceSync?,
    ): SourceSync {
        val accessToken = sourceSyncData.config.accessToken
        requireNotNull(accessToken) { "accessToken is required for processSourceSyncRegistration" }
        // not actually required, but good to test the token, and get the user id
        val memberUri = sourceConfigs.extractSourceConfig(SourceType.TRELLO.key, "memberUrl")

        // Call Trello to get User data
        val trelloMemberDto =
            trelloWebClient
                .get()
                .uri(memberUri)
                .attributes { attrs ->
                    attrs[IntegrationConstants.REQUEST_ATTRIBUTE_AUTHORIZATION_USER_TOKEN] =
                        accessToken
                }.retrieve()
                .bodyToMono(TrelloMemberDto::class.java)
                .awaitSingle()

        val resolvedSourceSync =
            existingSourceSync?.copy(
                config =
                    SourceSyncConfig(
                        accessToken = tokenEncryptionUtil.encrypt(accessToken),
                        sourceUserId = trelloMemberDto.id,
                    ),
                status = SourceSyncStatus.inProgress(),
            ) ?: SourceSync.initiate(
                sourceSyncData,
                requestingContributor,
                SourceSyncConfig(
                    accessToken = tokenEncryptionUtil.encrypt(accessToken),
                    sourceUserId = trelloMemberDto.id,
                ),
            )
        return trelloStepsFns[stepKeysInOrder[0]]?.let {
            it(
                resolvedSourceSync,
                requestingContributor,
            )
        }
            ?: throw IllegalArgumentException(
                "Trello Source Sync Strategy " +
                    "is not properly configured for ${stepKeysInOrder[0]}",
            )
    }

    override suspend fun isReadyForSyncing(
        sourceSync: SourceSync,
        requestingContributor: SimpleContributor,
    ): Boolean =
        sourceSync.config.steps.all { it.isCompleted() } &&
            sourceSync.config.steps
                .map { it.stepKey }
                .containsAll(TrelloSteps.entries.map { it.value })

    override suspend fun configureNextStepData(
        sourceSync: SourceSync,
        requestingContributor: SimpleContributor,
    ): SourceSync {
        val currentStep = sourceSync.config.steps.size
        return trelloStepsFns[stepKeysInOrder[currentStep]]?.let {
            it(
                sourceSync,
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
        suspend (SourceSync, SimpleContributor)
        -> SourceSync,
    > =
        mapOf(
            TrelloSteps.SELECT_BOARD to
                { sourceSync, _ ->
                    val accessToken = extractAccessToken(sourceSync)
                    val memberBoardsUri = sourceConfigs.extractSourceConfig(SourceType.TRELLO.key, "memberBoardsUrl")

                    // Call Trello to get member Boards
                    val boardsDto =
                        trelloWebClient
                            .get()
                            .uri(memberBoardsUri)
                            .attributes { attrs ->
                                attrs[IntegrationConstants.REQUEST_ATTRIBUTE_AUTHORIZATION_USER_TOKEN] =
                                    accessToken
                            }.retrieve()
                            .bodyToMono(typeReference<List<TrelloBoardDto>>())
                            .awaitSingle()
                    val boardsOptions = boardsDto.map { OptionSpec(it.id, it.name) }
                    val boardFieldSpec =
                        InlineFieldSpec(
                            TrelloResponseFieldKeys.SELECT_BOARD_FIELD.value,
                            FieldSpec.SELECT,
                            boardsOptions,
                        )
                    sourceSync
                        .addStep(
                            SourceSyncStatusStep(
                                TrelloSteps.SELECT_BOARD.value,
                                listOf(boardFieldSpec),
                            ),
                        )
                    sourceSync
                },
        )

    private val stepKeysInOrder: List<TrelloSteps> = trelloStepsFns.keys.toList()

    override suspend fun triggerSourceSync(
        sourceSync: SourceSync,
        requestingContributor: SimpleContributor,
        syncEventId: String,
    ): List<IntegrationAsset> {
        val accessToken = extractAccessToken(sourceSync)
        val boardCardsUrlPattern = sourceConfigs.extractSourceConfig(SourceType.TRELLO.key, "boardCardsUrlPattern")
        return obtainIntegrationAssets(
            sourceSync,
            accessToken,
            boardCardsUrlPattern,
            syncEventId,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun obtainIntegrationAssets(
        sourceSync: SourceSync,
        accessToken: String,
        boardCardsUrlPattern: String,
        syncEventId: String,
    ): List<IntegrationAsset> {
        requireNotNull(sourceSync.id) { "sourceSync.id is required for triggerSourceSync" }
        val trelloPluginId = sourceConfigs.extractSourceConfig(SourceType.TRELLO.key, "pluginId")
        val selectedBoardIds = extractStepData(sourceSync, TrelloSteps.SELECT_BOARD.value, TrelloResponseFieldKeys.SELECT_BOARD_FIELD.value)

        return selectedBoardIds
            .asFlow()
            .flatMapMerge { selectedBoardId ->
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
                                done = it.dueComplete == true,
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

    private fun extractStepData(
        sourceSync: SourceSync,
        stepKey: String,
        fieldKey: String,
    ): List<String> =
        sourceSync.config.steps
            .first { it.stepKey == stepKey }
            .responseData
            ?.get(fieldKey)
            ?: throw IllegalArgumentException("selected board is required for triggerSourceSync")

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
                    val pluginValueDto =
                        objectMapper.readValue(
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

    private fun extractAccessToken(sourceSync: SourceSync) =
        sourceSync.config.accessToken?.let {
            tokenEncryptionUtil.decrypt(
                it,
            )
        } ?: throw IllegalArgumentException(
            "accessToken could not be obtained from configuration",
        )

    private fun parseDueDate(due: String?): Instant? =
        try {
            due?.let { Instant.parse(due) }
        } catch (e: DateTimeParseException) {
            logger.error("Invalid due date format for Trello card: $due")
            null
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun obtainUsersMatchOptions(
        sourceSync: SourceSync,
        requestingContributor: SimpleContributor,
    ): List<SourceUser> {
        val accessToken = extractAccessToken(sourceSync)
        val boardMembersUrlPattern = sourceConfigs.extractSourceConfig(SourceType.TRELLO.key, "boardMembersUrlPattern")

        val selectedBoardIds =
            sourceSync.config.steps
                .first { it.stepKey == TrelloSteps.SELECT_BOARD.value }
                .responseData
                ?.get(TrelloResponseFieldKeys.SELECT_BOARD_FIELD.value)
        requireNotNull(selectedBoardIds) { "selected board is required for obtainUsersMatchOptions" }

        return selectedBoardIds
            .asFlow()
            .flatMapMerge { selectedBoardId ->
                try {
                    // Call Trello to get board cards List
                    fetchAllMembers(
                        trelloWebClient,
                        accessToken,
                        boardMembersUrlPattern,
                        selectedBoardId,
                    ).map { it.toSourceUser() }
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
            boardCardsUrlPattern
                .replace(":boardId", selectedBoardId)
                .replace(":limit", limit.toString())
                .replace(":since", since ?: "")
        return trelloWebClient
            .get()
            .uri(boardCardsUrl)
            .attributes { attrs ->
                attrs[IntegrationConstants.REQUEST_ATTRIBUTE_AUTHORIZATION_USER_TOKEN] =
                    accessToken
            }.retrieve()
            .bodyToFlow<TrelloCardDto>()
            .toList()
    }

    fun fetchPaginatedCards(since: String?): Flow<TrelloCardDto> =
        flow {
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
    return trelloWebClient
        .get()
        .uri(boardMembersUrl)
        .attributes { attrs ->
            attrs[IntegrationConstants.REQUEST_ATTRIBUTE_AUTHORIZATION_USER_TOKEN] =
                accessToken
        }.retrieve()
        .bodyToFlow<TrelloMemberDto>()
}

private fun TrelloMemberDto.toSourceUser(): SourceUser =
    SourceUser(
        sourceUserId = id,
        name = fullName,
        username = username,
        email = email,
        profileUrl = url,
        profileMediaUrl = avatarUrl?.let { "$it/50.png" },
    )

enum class TrelloSteps(
    val value: String,
) {
    SELECT_BOARD("SELECT_BOARD"),
}

private enum class TrelloResponseFieldKeys(
    val value: String,
) {
    SELECT_BOARD_FIELD("SELECT_BOARD_FIELD"),
}
