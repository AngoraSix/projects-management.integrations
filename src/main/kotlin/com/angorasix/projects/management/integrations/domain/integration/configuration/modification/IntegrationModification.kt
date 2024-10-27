import com.angorasix.commons.domain.SimpleContributor
import com.angorasix.commons.domain.modification.DomainObjectModification
import com.angorasix.projects.management.integrations.domain.integration.configuration.Integration
import com.angorasix.projects.management.integrations.domain.integration.configuration.IntegrationStatusValues
import java.time.Instant

abstract class IntegrationModification<U>(modifyValue: U) : DomainObjectModification<Integration, U>(modifyValue)

class ModifyIntegrationStatus(status: IntegrationStatusValues) : IntegrationModification<IntegrationStatusValues>(status) {
    override fun modify(simpleContributor: SimpleContributor, domainObject: Integration): Integration {
        require(domainObject.isAdmin(simpleContributor.contributorId)) { "Can't add this member" }
        domainObject.status.status = modifyValue
        domainObject.status.expirationDate = Instant.now()
        return domainObject
    }
}