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
data class ListSourceSyncFilter(
    val ids: Collection<String>? = null, // integration ids
    val sources: Set<String>? = null,
    val integrationId: Collection<String>? = null,
) {
    fun toMultiValueMap(): MultiValueMap<String, String> {
        val multiMap: MultiValueMap<String, String> = LinkedMultiValueMap()
        integrationId?.let { multiMap.add("integrationId", integrationId.joinToString(",")) }
        sources?.let { multiMap.add("sources", sources.joinToString(",")) }
        ids?.let { multiMap.add("ids", ids.joinToString(",")) }
        return multiMap
    }
}
