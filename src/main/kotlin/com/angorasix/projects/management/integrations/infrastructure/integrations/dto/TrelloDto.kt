package com.angorasix.projects.management.integrations.infrastructure.integrations.dto

data class TrelloMemberDto(
    val id: String,
    val username: String,
    val fullName: String?,
    val url: String?,
    val email: String?,
    val avatarUrl: String?,
)

data class TrelloBoardDto(
    val id: String,
    val name: String,
    val desc: String?,
    val closed: Boolean?,
    val idOrganization: String?,
    val pinned: Boolean?,
    val url: String?,
    val shortUrl: String?,
    val shortLink: String?,
)

data class TrelloListDto(
    val id: String,
    val name: String,
    val closed: Boolean?,
    val pos: Int?,
)

data class TrelloCardDto(
    val id: String,
    val name: String,
    val desc: String?,
    val closed: Boolean?,
    val idList: String?,
    val url: String?,
    val shortUrl: String?,
    val shortLink: String?,
    val due: String?,
    val dueComplete: Boolean?,
    val idMembers: List<String>?,
)
