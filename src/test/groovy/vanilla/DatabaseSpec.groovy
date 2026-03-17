package vanilla

import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
import vanilla.dto.QuoteParams
import vanilla.service.QuoteService

@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class DatabaseSpec extends Specification {

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
        quantities                  | expected
        ['W1': 3]                   | 369
        ['W1': 3, 'W2': 4, 'W3': 5] | 123 * 3 + 234 * 4 + 345 * 5
    }

    @Unroll
    def "price history should be saved"() {
        given:
        assert quoteService != null: "quoteService not injected"
        def params = new QuoteParams(quantities)

        when:
        def actual = quoteService.getQuote(params)

        then:
        actual.getTotalCents() == expected

        where:
        quantities | expected
        ['W1': 3]  | 369
    }

}
