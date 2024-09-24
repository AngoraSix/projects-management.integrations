package com.angorasix.projects.management.integrations.infrastructure.integrations.dto

data class BoardDto(
    val id: String,
    val name: String,
    val desc: String?,
    val closed: Boolean,
    val idOrganization: String,
    val pinned: Boolean,
    val url: String,
    val shortUrl: String,
    val shortLink: String,
)

data class MemberDto(
    val id: String,
    val username: String,
    val fullName: String,
    val url: String,
    val email: String,
    val avatarUrl: String)
