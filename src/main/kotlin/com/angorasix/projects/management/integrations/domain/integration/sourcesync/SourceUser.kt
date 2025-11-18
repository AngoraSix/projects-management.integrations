package com.angorasix.projects.management.integrations.domain.integration.sourcesync

data class SourceUser(
    val sourceUserId: String,
    val username: String?,
    val email: String?,
    val name: String?,
    val profileMediaUrl: String?,
    val profileUrl: String?,
) {
    fun toMap(): Map<String, Any> =
        listOfNotNull(
            "sourceUserId" to sourceUserId, // always non-null
            username?.let { "username" to it },
            email?.let { "email" to it },
            name?.let { "name" to it },
            profileMediaUrl?.let { "profileMediaUrl" to it },
            profileUrl?.let { "profileUrl" to it },
        ).toMap()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SourceUser

        return sourceUserId == other.sourceUserId
    }

    override fun hashCode(): Int = sourceUserId.hashCode()
}
