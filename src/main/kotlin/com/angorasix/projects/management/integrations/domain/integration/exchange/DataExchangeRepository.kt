package com.angorasix.projects.management.integrations.domain.integration.exchange

import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.data.repository.kotlin.CoroutineSortingRepository

/**
 *
 * Integration Data Exchange Repository.
 *
 * @author rozagerardo
 */
interface DataExchangeRepository :
    CoroutineCrudRepository<DataExchange, String>,
    CoroutineSortingRepository<DataExchange, String> {
}
