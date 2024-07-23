//package com.angorasix.projects.management.integrations.utils
//
//import com.angorasix.commons.domain.SimpleContributor
//import com.angorasix.projects.management.core.domain.management.Bylaw
//import com.angorasix.projects.management.core.domain.management.BylawWellknownScope
//import com.angorasix.projects.management.core.domain.management.ManagementConstitution
//import com.angorasix.projects.management.core.domain.management.ManagementStatus
//import com.angorasix.projects.management.core.domain.management.ProjectManagement
//import com.angorasix.projects.management.core.presentation.dto.BylawDto
//import com.angorasix.projects.management.core.presentation.dto.ManagementConstitutionDto
//import com.angorasix.projects.management.core.presentation.dto.ProjectManagementDto
//import java.util.*
//
///**
// * <p>
// * </p>
// *
// * @author rozagerardo
// */
//fun mockProjectManagement(
//    modifier: String = "",
//    admins: Set<SimpleContributor> = emptySet(),
//): ProjectManagement = ProjectManagement(
//    "mockedProjectId$modifier",
//    admins,
//    mockConstitution(),
//    ManagementStatus.STARTUP,
//)
//
//fun mockConstitution(): ManagementConstitution = ManagementConstitution(
//    listOf(
//        Bylaw(
//            BylawWellknownScope.OPERATION_CORE_RETRIBUTION_MODEL.name,
//            "CAPS",
//        ),
//        Bylaw(BylawWellknownScope.OWNERSHIP_MECHANISM.name, "CAPS-BASED"),
//    ),
//)
//
//fun mockProjectManagementDto(modifier: String = getRandomString(5)): ProjectManagementDto = ProjectManagementDto(
//    "mockedProjectId$modifier",
//    emptySet(),
//    mockConstitutionDto(),
//    ManagementStatus.STARTUP,
//)
//
//private fun getRandomString(length: Int): String {
//    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
//    return (1..length).map { allowedChars.random() }.joinToString("")
//}
//
//fun mockConstitutionDto(): ManagementConstitutionDto = ManagementConstitutionDto(
//    listOf(
//        BylawDto(
//            BylawWellknownScope.OPERATION_CORE_RETRIBUTION_MODEL.name,
//            "CAPS",
//        ),
//        BylawDto(BylawWellknownScope.OWNERSHIP_MECHANISM.name, "CAPS-BASED"),
//    ),
//)
//
//fun mockRequestingContributorHeader(asAdmin: Boolean = false): String {
//    val requestingContributorJson =
//        """
//            {
//              "contributorId": "mockedContributorId1",
//              "projectAdmin": $asAdmin
//            }
//        """.trimIndent()
//    return Base64.getUrlEncoder().encodeToString(requestingContributorJson.toByteArray())
//}
