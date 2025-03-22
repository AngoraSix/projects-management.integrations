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
data class ListIntegrationAssetFilter(
    val ids: Collection<String>? = null, // internal asset ids
    val assetDataId: Collection<String>? = null, // external asset ids
    val sourceSyncId: Collection<String>? = null,
    val sources: Set<String>? = null,
) {
    fun toMultiValueMap(): MultiValueMap<String, String> {
        val multiMap: MultiValueMap<String, String> = LinkedMultiValueMap()
        assetDataId?.let { multiMap.add("assetDataId", assetDataId.joinToString(",")) }
        sourceSyncId?.let { multiMap.add("sourceSyncId", sourceSyncId.joinToString(",")) }
        sources?.let { multiMap.add("sources", sources.joinToString(",")) }
        ids?.let { multiMap.add("ids", ids.joinToString(",")) }
        return multiMap
    }
}
