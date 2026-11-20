package vanilla.contracts;

import io.restassured.module.webtestclient.RestAssuredWebTestClient;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for all Spring Cloud Contract generated verifier tests (WEBTESTCLIENT mode).
 *
 * <p>Uses RestAssured's {@code spring-web-test-client} module which wraps Spring's {@link
 * org.springframework.test.web.reactive.server.WebTestClient}. This avoids both Spring Framework 7
 * API incompatibilities present in MOCKMVC and EXPLICIT modes with spring-cloud-contract 4.x:
 *
 * <ul>
 *   <li>MOCKMVC: {@code MockHttpServletRequestBuilder.header(String, Object[])} removed in Spring 7
 *   <li>EXPLICIT: RestAssured 5.x Groovy internals conflict with the project's Groovy 5.x
 * </ul>
 *
 * <p>Flyway is run directly against the Testcontainer so the full seeded schema is available before
 * the application context starts. Update to spring-cloud-contract 5.x when the Spring Cloud
 * 2025.0.x release train ships for Spring Boot 4.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public abstract class ContractVerifierBase {

  @SuppressWarnings("resource")
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("testdb")
          .withUsername("testuser")
          .withPassword("testpass")
          .withReuse(true);

  static {
    POSTGRES.start();
    Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .locations("classpath:db/migration")
        .baselineOnMigrate(true)
        .load()
        .migrate();
  }

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired WebApplicationContext context;

  @BeforeEach
  void setup() {
    // Build a WebTestClient backed by MockMvc rather than calling webAppContextSetup(),
    // which requires WebFlux's webHandler bean (not present in a Spring MVC application).
    RestAssuredWebTestClient.webTestClient(
        MockMvcWebTestClient.bindToApplicationContext(context).build());
  }
}
