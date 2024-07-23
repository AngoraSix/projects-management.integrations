//package com.angorasix.projects.management.integrations.integration
//
//import com.angorasix.projects.management.core.domain.management.ProjectManagement
//import com.angorasix.projects.management.core.infrastructure.config.configurationproperty.api.ApiConfigs
//import com.angorasix.projects.management.core.integration.utils.IntegrationProperties
//import com.angorasix.projects.management.core.integration.utils.initializeMongodb
//import com.angorasix.projects.management.core.presentation.dto.ProjectManagementDto
//import com.angorasix.projects.management.core.presentation.dto.ProjectsManagementQueryParams
//import com.angorasix.projects.management.core.utils.mockProjectManagementDto
//import com.angorasix.projects.management.core.utils.mockRequestingContributorHeader
//import com.fasterxml.jackson.databind.ObjectMapper
//import kotlinx.coroutines.runBlocking
//import org.hamcrest.Matchers.greaterThanOrEqualTo
//import org.hamcrest.Matchers.hasItems
//import org.hamcrest.Matchers.startsWith
//import org.junit.jupiter.api.BeforeAll
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.fail
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.context.properties.EnableConfigurationProperties
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.data.mongodb.core.ReactiveMongoTemplate
//import org.springframework.data.mongodb.core.query.Criteria
//import org.springframework.data.mongodb.core.query.Query
//import org.springframework.hateoas.MediaTypes
//import org.springframework.http.HttpStatus
//import org.springframework.http.MediaType
//import org.springframework.test.context.TestPropertySource
//import org.springframework.test.web.reactive.server.WebTestClient
//import reactor.core.publisher.Mono
//
//@SpringBootTest(
//    classes = [com.angorasix.projects.management.integrations.ProjectsManagementCoreApplication::class],
//    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
//)
//@TestPropertySource(locations = ["classpath:integration-application.properties"])
//@EnableConfigurationProperties(IntegrationProperties::class)
//class ProjectsManagementIntegrationsIntegrationTest(
//    @Autowired val mongoTemplate: ReactiveMongoTemplate,
//    @Autowired val mapper: ObjectMapper,
//    @Autowired val properties: IntegrationProperties,
//    @Autowired val webTestClient: WebTestClient,
//    @Autowired val apiConfigs: ApiConfigs,
//) {
//
//    @BeforeAll
//    fun setUp() = runBlocking {
//        initializeMongodb(
//            properties.mongodb.baseJsonFile,
//            mongoTemplate,
//            mapper,
//        )
//    }
//
//    @Test
//    fun `given base data - when call Get Project Management list - then return all persisted projects`() {
//        webTestClient.get()
//            .uri("/projects-management")
//            .accept(MediaType.APPLICATION_JSON)
//            .exchange()
//            .expectStatus().isOk.expectBody()
//            .jsonPath("$.size()").value(greaterThanOrEqualTo(2))
//            .jsonPath("$..id").exists()
//            .jsonPath("$..projectId").value(hasItems("123withSingleSection", "345MultipleSections"))
//            .jsonPath("$..constitution..bylaws.size()")
//            .value(greaterThanOrEqualTo(2))
//            .jsonPath("$..status")
//            .value(hasItems(("STARTUP")))
//    }
//
//    @Test
//    fun `given base data - when call Get Project Management list filtering by projectId - then return filtered persisted projects`() {
//        webTestClient.get()
//            .uri { builder ->
//                builder.path("/projects-management").queryParam(
//                    ProjectsManagementQueryParams.PROJECT_IDS.param,
//                    "123withSingleSection",
//                ).build()
//            }
//            .accept(MediaType.APPLICATION_JSON)
//            .exchange()
//            .expectStatus().isOk.expectBody()
//            .jsonPath("$.size()").value(greaterThanOrEqualTo(1))
//            .jsonPath("$..projectId").value(hasItems("123withSingleSection"))
//    }
//
//    @Test
//    fun `given base data - when retrieve Project Management by id - then existing is retrieved`() {
//        val initElementQuery = Query()
//        initElementQuery.addCriteria(
//            Criteria.where("projectId")
//                .`is`("123withSingleSection"),
//        )
//        val elementId =
//            mongoTemplate.findOne(initElementQuery, ProjectManagement::class.java).block()?.id
//
//        webTestClient.get()
//            .uri("/projects-management/{projectManagementId}", elementId)
//            .accept(MediaType.APPLICATION_JSON)
//            .exchange()
//            .expectStatus().isOk.expectBody()
//            .jsonPath("$.id")
//            .exists()
//            .jsonPath("$.projectId")
//            .isEqualTo("123withSingleSection")
//            .jsonPath("$.constitution.bylaws.size()")
//            .value(greaterThanOrEqualTo(1))
//            .jsonPath("$.status")
//            .isEqualTo("STARTUP")
//    }
//
//    @Test
//    fun `given base data - when get non-existing Management - then 404 response`() {
//        webTestClient.get()
//            .uri("/projects-management/non-existing-id")
//            .accept(MediaType.APPLICATION_JSON)
//            .exchange()
//            .expectStatus().isNotFound
//    }
//
//    @Test
//    fun `when post new Project Management - then new project management is persisted`() {
//        val projectManagementBody = mockProjectManagementDto()
//        webTestClient.post()
//            .uri("/projects-management")
//            .accept(MediaType.APPLICATION_JSON)
//            .contentType(MediaTypes.HAL_FORMS_JSON)
//            .header(apiConfigs.headers.contributor, mockRequestingContributorHeader(true))
//            .body(
//                Mono.just(projectManagementBody),
//                ProjectManagementDto::class.java,
//            )
//            .exchange()
//            .expectStatus().isCreated.expectBody()
//            .jsonPath("$.id").exists()
//            .jsonPath("$.projectId").value(startsWith("mockedProjectId"))
//            .jsonPath("$.constitution.bylaws.size()")
//            .value(greaterThanOrEqualTo(2))
//            .jsonPath("$.status")
//            .isEqualTo("STARTUP")
//    }
//
//    @Test
//    fun `given new persisted management - when retrieved - then data matches`() {
//        val projectManagementBody = mockProjectManagementDto()
//        val newProjectManagement = webTestClient.post()
//            .uri("/projects-management")
//            .accept(MediaType.APPLICATION_JSON)
//            .contentType(MediaTypes.HAL_FORMS_JSON)
//            .header(apiConfigs.headers.contributor, mockRequestingContributorHeader(true))
//            .body(
//                Mono.just(projectManagementBody),
//                ProjectManagementDto::class.java,
//            )
//            .exchange()
//            .expectStatus().isCreated
//            .expectBody(ProjectManagementDto::class.java)
//            .returnResult().responseBody ?: fail("Create operation retrieved empty response")
//
//        webTestClient.get()
//            .uri("/projects-management/${newProjectManagement.id}")
//            .accept(MediaType.APPLICATION_JSON)
//            .exchange()
//            .expectStatus().isOk.expectBody()
//            .jsonPath("$.id").isEqualTo(newProjectManagement.id!!)
//            .jsonPath("$.projectId").value(startsWith("mockedProjectId"))
//            .jsonPath("$.status").isEqualTo("STARTUP")
//            .jsonPath("$.constitution.bylaws.size()").isEqualTo(2)
//    }
//
//    @Test
//    fun `when post new Project Management without sections - then Created response`() {
//        val projectManagementBody = """
//            {
//                "projectId": "projectId456",
//                "constitution": {
//                    "bylaws": [
//                        {
//                            "scope": "ANY",
//                            "definition": "Any rule"
//                        }
//                    ]
//                },
//                "status": "STARTUP"
//            }
//        """.trimIndent()
//        webTestClient.post()
//            .uri("/projects-management")
//            .accept(MediaType.APPLICATION_JSON)
//            .contentType(MediaTypes.HAL_FORMS_JSON)
//            .header(apiConfigs.headers.contributor, mockRequestingContributorHeader(true))
//            .body(
//                Mono.just(projectManagementBody),
//                String::class.java,
//            )
//            .exchange()
//            .expectStatus().isCreated
//    }
//
//    @Test
//    fun `when post new Project Management without constitution - then Bad Request response`() {
//        val projectManagementBody = """
//            {
//              "projectId": "projectId456",
//              "constitution": null
//            }
//        """.trimIndent()
//        webTestClient.post()
//            .uri("/projects-management")
//            .contentType(MediaTypes.HAL_FORMS_JSON)
//            .accept(MediaType.APPLICATION_JSON)
//            .header(apiConfigs.headers.contributor, mockRequestingContributorHeader(true))
//            .body(
//                Mono.just(projectManagementBody),
//                String::class.java,
//            )
//            .exchange()
//            .expectStatus().isBadRequest
//            .expectBody()
//            .jsonPath("$.errorCode").isEqualTo("PROJECT_MANAGEMENT_INVALID")
//            .jsonPath("$.error").exists()
//            .jsonPath("$.status").isEqualTo(HttpStatus.BAD_REQUEST.value())
//            .jsonPath("$.message").isEqualTo("ProjectManagement constitution expected")
//    }
//
//    @Test
//    fun `when post new Project Management with empty sections - then Bad Request response`() {
//        val projectManagementBody = mockProjectManagementDto()
//        webTestClient.post()
//            .uri("/projects-management")
//            .accept(MediaType.APPLICATION_JSON)
//            .contentType(MediaTypes.HAL_FORMS_JSON)
//            .header(apiConfigs.headers.contributor, mockRequestingContributorHeader(true))
//            .body(
//                Mono.just(projectManagementBody),
//                ProjectManagementDto::class.java,
//            )
//            .exchange()
//            .expectStatus().isCreated
//    }
//}
