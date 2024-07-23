package com.angorasix.projects.management.integrations.infrastructure.persistence.repository

data class BulkResult(
    val inserted: Int,
    val modified: Int,
)