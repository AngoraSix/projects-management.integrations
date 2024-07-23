package com.angorasix.projects.management.integrations.infrastructure.persistence.repository

import com.angorasix.projects.management.integrations.domain.integration.asset.IntegrationAsset
import com.angorasix.projects.management.integrations.infrastructure.queryfilters.ListIntegrationFilter
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Query
import reactor.core.publisher.Flux

/**
 * <p>
 * </p>
 *
 * @author rozagerardo
 */
@ExperimentalCoroutinesApi
@ExtendWith(MockKExtension::class)
class IntegrationAssetInfraRepositoryImplUnitTest {

    private lateinit var filterRepoImpl: IntegrationInfraRepository

    @MockK
    private lateinit var mongoOps: ReactiveMongoOperations

    val slot = slot<Query>()

    @BeforeEach
    fun init() {
        filterRepoImpl = IntegrationInfraRepositoryImpl(mongoOps)
    }

    @Test
    @Throws(Exception::class)
    fun `Given empty ProjectFilter - When findUsingFilter - Then find repo operation with empty query`() =
        runTest {
            val filter = ListIntegrationFilter()
            val mockedFlux = mockk<Flux<IntegrationAsset>>()
            every {
                mongoOps.find(
                    capture(slot),
                    IntegrationAsset::class.java,
                )
            } returns mockedFlux

            filterRepoImpl.findUsingFilter(filter)

            val capturedQuery = slot.captured

            verify { mongoOps.find(capturedQuery, ListIntegrationFilter::class.java) }
            assertThat(capturedQuery.queryObject).isEmpty()
        }

    @Test
    @Throws(Exception::class)
    fun `Given populated ProjectFilter - When findUsingFilter - Then find repo operation with populated query`() =
        runTest {
            val filter = ListIntegrationFilter(listOf("1", "2"))
            val mockedFlux = mockk<Flux<IntegrationAsset>>()
            every {
                mongoOps.find(
                    capture(slot),
                    IntegrationAsset::class.java,
                )
            } returns mockedFlux

            filterRepoImpl.findUsingFilter(filter)

            val capturedQuery = slot.captured

            verify { mongoOps.find(capturedQuery, IntegrationAsset::class.java) }
            assertThat(capturedQuery.queryObject).containsKey("projectId")
        }
}
