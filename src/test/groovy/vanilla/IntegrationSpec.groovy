package vanilla

import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.spock.Testcontainers
import vanilla.model.QuoteParams
import vanilla.service.QuoteService
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static vanilla.model.Product.BATTERY

@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class IntegrationSpec extends Specification {

    @Shared
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")

    def setupSpec() {
        postgres.start()

        Flyway flyway = Flyway.configure()
                .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
                .locations("classpath:db/migration") // Default location
                .load()

        flyway.migrate()
    }
//
//    static {
//        postgres.start()
//    }

//    @DynamicPropertySource
//    static void registerPgProperties(DynamicPropertyRegistry registry) {
//        registry.add("spring.datasource.url", postgres::getJdbcUrl)
//        registry.add("spring.datasource.username", postgres::getUsername)
//        registry.add("spring.datasource.password", postgres::getPassword)
//    }

    @Autowired
    QuoteService quoteService

    @Unroll
    def "quote(#quantities) should be #expected"() {
        given:
        assert quoteService != null: "quoteService not injected"
        def params = new QuoteParams(quantities)

        when:
        def actual = quoteService.getQuote(params)

        then:
        actual.getTotalCents() == expected

        where:
        quantities     | expected
        [(BATTERY): 3] | 103701
    }
}
