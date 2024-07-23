//package com.angorasix.projects.management.integrations.integration.docs
//
//import com.angorasix.projects.management.integrations.ProjectsManagementCoreApplication
//import com.angorasix.projects.management.core.domain.management.Bylaw
//import com.angorasix.projects.management.core.domain.management.ManagementConstitution
//import com.angorasix.projects.management.core.domain.management.ManagementStatus
//import com.angorasix.projects.management.core.domain.management.ProjectManagement
//import com.angorasix.projects.management.core.infrastructure.config.configurationproperty.api.ApiConfigs
//import com.angorasix.projects.management.core.integration.utils.IntegrationProperties
//import com.angorasix.projects.management.core.integration.utils.initializeMongodb
//import com.angorasix.projects.management.core.utils.mockProjectManagementDto
//import com.angorasix.projects.management.core.utils.mockRequestingContributorHeader
//import com.fasterxml.jackson.databind.ObjectMapper
//import kotlinx.coroutines.runBlocking
//import org.junit.jupiter.api.BeforeAll
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.extension.ExtendWith
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.context.properties.EnableConfigurationProperties
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.context.ApplicationContext
//import org.springframework.data.mongodb.core.ReactiveMongoTemplate
//import org.springframework.data.mongodb.core.query.Criteria
//import org.springframework.data.mongodb.core.query.Query
//import org.springframework.hateoas.MediaTypes
//import org.springframework.http.HttpHeaders
//import org.springframework.http.MediaType
//import org.springframework.restdocs.RestDocumentationContextProvider
//import org.springframework.restdocs.RestDocumentationExtension
//import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
//import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
//import org.springframework.restdocs.hypermedia.HypermediaDocumentation.halLinks
//import org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel
//import org.springframework.restdocs.hypermedia.HypermediaDocumentation.links
//import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
//import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
//import org.springframework.restdocs.payload.FieldDescriptor
//import org.springframework.restdocs.payload.JsonFieldType
//import org.springframework.restdocs.payload.PayloadDocumentation.beneathPath
//import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
//import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
//import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
//import org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath
//import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
//import org.springframework.restdocs.request.RequestDocumentation.pathParameters
//import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document
//import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.documentationConfiguration
//import org.springframework.test.context.TestPropertySource
//import org.springframework.test.web.reactive.server.WebTestClient
//import org.springframework.test.web.reactive.server.body
//import org.springframework.web.reactive.function.client.ExchangeFilterFunction
//import reactor.core.publisher.Mono
//import java.time.Duration
//
//@ExtendWith(RestDocumentationExtension::class)
//@SpringBootTest(
//    classes = [com.angorasix.projects.management.integrations.ProjectsManagementCoreApplication::class],
//    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
//)
//@TestPropertySource(locations = ["classpath:integration-application.properties"])
//@EnableConfigurationProperties(IntegrationProperties::class)
//class IntegrationDocsIntegrationTest(
//    @Autowired val mongoTemplate: ReactiveMongoTemplate,
//    @Autowired val mapper: ObjectMapper,
//    @Autowired val properties: IntegrationProperties,
//    @Autowired val apiConfigs: ApiConfigs,
//) {
//
//    private lateinit var webTestClient: WebTestClient
//
//    var bylawDescription = arrayOf<FieldDescriptor>(
//        fieldWithPath("scope").description("'key' field identifying the aspect defined by the bylaw"),
//        fieldWithPath("definition").description("Actual definition of the rule"),
//    )
//
//    var constitutionDescription = arrayOf<FieldDescriptor>(
//        subsectionWithPath("bylaws[]").type(ArrayOfFieldType(Bylaw::class.simpleName))
//            .description("Array of the associated bylaws that define the management constitution"),
//    )
//
//    var projectManagementDescriptor = arrayOf<FieldDescriptor>(
//        fieldWithPath("id").description("Project Management identifier"),
//        fieldWithPath("projectId").description("Identifier of the associated Project Id"),
//        subsectionWithPath("constitution").type(ManagementConstitution::class.simpleName)
//            .description("The Project Management constitution containing all the foundational management rules"),
//        subsectionWithPath("status").type(ManagementStatus::class.simpleName)
//            .description("The current status of the Project Management"),
//
//        subsectionWithPath("links").optional().description("HATEOAS links")
//            .type(JsonFieldType.ARRAY), // until we resolve and unify the list and single response links, all will be marked as optional
//        subsectionWithPath("_links").optional().description("HATEOAS links")
//            .type(JsonFieldType.OBJECT),
//        subsectionWithPath("_templates").optional()
//            .description("HATEOAS HAL-FORM links template info").type(
//                JsonFieldType.OBJECT,
//            ),
//    )
//
//    var projectManagementPostBodyDescriptor = arrayOf<FieldDescriptor>(
//        fieldWithPath("id").description("Project Management identifier"),
//        fieldWithPath("projectId").description("Identifier of the associated Project Id"),
//        subsectionWithPath("constitution").type(ManagementConstitution::class.simpleName)
//            .description("The Project Management constitution containing all the foundational management rules"),
//        subsectionWithPath("status").type(ManagementStatus::class.simpleName)
//            .description("The current status of the Project Management"),
//        fieldWithPath("links[]").ignored(),
//    )
//
//    @BeforeAll
//    fun setUpDb() = runBlocking {
//        initializeMongodb(
//            properties.mongodb.baseJsonFile,
//            mongoTemplate,
//            mapper,
//        )
//    }
//
//    @BeforeEach
//    fun setUpWebClient(
//        applicationContext: ApplicationContext,
//        restDocumentation: RestDocumentationContextProvider,
//    ) = runBlocking {
//        webTestClient = WebTestClient.bindToApplicationContext(applicationContext)
//            .configureClient()
//            .responseTimeout(Duration.ofMillis(30000))
//            .filter(
//                documentationConfiguration(restDocumentation),
//            )
//            .filter(
//                ExchangeFilterFunction.ofRequestProcessor { clientRequest ->
//                    println(
//                        "Request: ${clientRequest.method()} ${clientRequest.url()}",
//                    )
//                    clientRequest.headers()
//                        .forEach { name, values ->
//                            values.forEach { value ->
//                                println(
//                                    "$name=$value",
//                                )
//                            }
//                        }
//                    Mono.just(clientRequest)
//                },
//            )
//            .build()
//    }
//
//    @Test
//    fun `Given persisted projects - When execute and document requests - Then everything documented`() {
//        executeAndDocumentGetListProjectsRequest()
//        executeAndDocumentGetSingleProjectRequest()
//        executeAndDocumentPostCreateProjectRequest()
//    }
//
//    private fun executeAndDocumentPostCreateProjectRequest() {
//        val newProjectManagement = mockProjectManagementDto()
//        webTestClient.post()
//            .uri(
//                "/management-core",
//            )
//            .accept(MediaType.APPLICATION_JSON)
//            .contentType(MediaTypes.HAL_FORMS_JSON)
//            .header(apiConfigs.headers.contributor, mockRequestingContributorHeader(true))
//            .body(Mono.just(newProjectManagement))
//            .exchange()
//            .expectStatus().isCreated.expectBody()
//            .consumeWith(
//                document(
//                    "project-create",
//                    preprocessResponse(prettyPrint()),
//                    requestFields(*projectManagementPostBodyDescriptor),
//                    responseHeaders(
//                        headerWithName(HttpHeaders.LOCATION).description("URL of the newly created project"),
//                    ),
//                    links(
//                        halLinks(),
//                        linkWithRel("self").description("The self link"),
//                        linkWithRel("updateProjectManagement").description("The link for the edit Project Management operation"),
//                    ),
//                    responseFields(*projectManagementDescriptor),
//                ),
//            )
//    }
//
//    private fun executeAndDocumentGetSingleProjectRequest() {
//        val initElementQuery = Query()
//        initElementQuery.addCriteria(
//            Criteria.where("projectId")
//                .`is`("123withSingleSection"),
//        )
//        val elementId =
//            mongoTemplate.findOne(initElementQuery, ProjectManagement::class.java).block()?.id
//
//        webTestClient.get()
//            .uri(
//                "/management-core/{projectManagementId}",
//                elementId,
//            )
//            .accept(MediaType.APPLICATION_JSON)
//            .exchange()
//            .expectStatus().isOk.expectBody()
//            .consumeWith(
//                document(
//                    "project-single",
//                    preprocessResponse(prettyPrint()),
//                    pathParameters(parameterWithName("projectManagementId").description("The Project Presntation id")),
//                    responseFields(*projectManagementDescriptor),
//                ),
//            )
//    }
//
//    private fun executeAndDocumentGetListProjectsRequest() {
//        webTestClient.get()
//            .uri("/management-core")
//            .accept(MediaType.APPLICATION_JSON)
//            .exchange()
//            .expectStatus().isOk.expectBody()
//            .consumeWith(
//                document(
//                    "project-list",
//                    preprocessResponse(prettyPrint()),
//                    responseFields(
//                        fieldWithPath("[]").type(ArrayOfFieldType(ProjectManagement::class.simpleName))
//                            .description("An array of projects"),
//                    ).andWithPrefix(
//                        "[].",
//                        *projectManagementDescriptor,
//                    ),
//                    responseFields(
//                        beneathPath("[].constitution").withSubsectionId("constitution"),
//                        *constitutionDescription,
//                    ),
//                    responseFields(
//                        beneathPath("[].constitution.bylaws[]").withSubsectionId("bylaw"),
//                    ).andWithPrefix(
//                        "[].",
//                        *bylawDescription,
//                    ),
//                ),
//            )
//    }
//
//    private class ArrayOfFieldType(private val field: String?) {
//        override fun toString(): String = "Array of $field"
//    }
//}
