package vanilla

import org.flywaydb.core.Flyway
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer
import spock.lang.Shared
import spock.lang.Specification

@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class AbstractIntegrationSpec extends Specification {

    @Shared
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")

    def setupSpec() {
        if (!postgres.running) {
            postgres.start()
            Flyway.configure()
                    .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
                    .locations("classpath:db/migration")
                    .load()
                    .migrate()
        }
    }
}
