package com.angorasix.projects.management.integrations.infrastructure.queryfilters

import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap

/**
 * <p>
 *     Classes containing different Request Query Filters.
 * </p>
 *
 * @author rozagerardo
 */
data class ListIntegrationFilter(
    val projectManagementId: Collection<String>? = null,
    val adminId: Set<String>? = null,
    val sources: Set<String>? = null,
    val ids: Collection<String>? = null, // integration ids
) {
    fun toMultiValueMap(): MultiValueMap<String, String> {
        val multiMap: MultiValueMap<String, String> = LinkedMultiValueMap()
        projectManagementId?.let { multiMap.add("projectManagementId", projectManagementId.joinToString(",")) }
        adminId?.let { multiMap.add("adminId", adminId.joinToString(",")) }
        sources?.let { multiMap.add("sources", sources.joinToString(",")) }
        adminId?.let { multiMap.add("ids", adminId.joinToString(",")) }
        return multiMap
    }
}
