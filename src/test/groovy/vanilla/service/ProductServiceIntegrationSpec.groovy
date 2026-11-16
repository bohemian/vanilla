package vanilla.service

import org.flywaydb.core.Flyway
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification
import vanilla.Application
import vanilla.entity.Product
import vanilla.repository.ProductRepository

@SpringBootTest(classes = Application, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class ProductServiceIntegrationSpec extends Specification {

    @Shared
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")

    def setupSpec() {
        postgres.start()

        Flyway.configure()
                .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
                .locations("classpath:db/migration")
                .load()
                .migrate()
    }

    @Autowired
    ProductService productService

    @Autowired
    ProductRepository productRepository

    def "getActiveProductCodes returns all seeded products which all have positive prices"() {
        when:
        def result = productService.getActiveProductCodes()

        then:
        result.containsAll(['W1', 'W2', 'W3'])
    }

    def "getActiveProductCodes excludes products with zero price"() {
        given:
        def freeProduct = productRepository.save(new Product(code: 'FREE', name: 'Free Widget', priceCents: 0))

        when:
        def result = productService.getActiveProductCodes()

        then:
        !result.contains('FREE')

        cleanup:
        productRepository.delete(freeProduct)
    }
}
