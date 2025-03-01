package com.angorasix.projects.management.integrations

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * Application context test.
 *
 * @author rozagerardo
 */
@SpringBootTest(
    properties = [
        "spring.data.mongodb.uri=mongodb://" + "\${embedded.mongodb.host}:\${embedded.mongodb.port}/" +
            "\${embedded.mongodb.database}",
    ],
)
class ProjectsManagementIntegrationsApplicationTest {
    @Test
    fun contextLoads() {
        // Empty method just to check that context is build ok
    }
}
