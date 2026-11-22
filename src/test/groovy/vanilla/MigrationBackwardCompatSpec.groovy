package vanilla

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.PostgreSQLContainer
import spock.lang.Shared
import spock.lang.Specification

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Guards the "DB migrations are backward-compatible" checklist item with an
 * automated signal. Runs the application against the old schema (without new
 * migrations) and verifies it starts and handles basic requests without
 * schema errors (missing columns, missing tables, type mismatches).
 *
 * Triggered by CI when a PR adds new migration files:
 *   ./gradlew migrationCompatTest -PflywayTarget=<N>
 * where N is the highest migration version on the base branch.
 *
 * Without -PflywayTarget (e.g. local dev), the task runs against all
 * migrations and behaves as a plain smoke test.
 *
 * Uses a dedicated container (no withReuse) so the database is always
 * freshly created and unaffected by other test runs. Does NOT extend
 * AbstractIntegrationSpec — that class applies all migrations manually,
 * which would defeat the purpose of this test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MigrationBackwardCompatSpec extends Specification {

    @Shared
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("testdb-compat")
            .withUsername("testuser")
            .withPassword("testpass")

    @LocalServerPort
    int port

    @Shared
    HttpClient httpClient = HttpClient.newHttpClient()

    private HttpResponse<String> get(String path) {
        def request = HttpRequest.newBuilder(URI.create("http://localhost:${port}${path}")).GET().build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private HttpResponse<String> post(String path, String body) {
        def request = HttpRequest.newBuilder(URI.create("http://localhost:${port}${path}"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    def "application starts against old schema"() {
        when:
        def response = get("/actuator/health/liveness")

        then:
        response.statusCode() == 200
    }

    def "GET /products executes without schema errors"() {
        when:
        def response = get("/products?maxPriceCents=10000")

        then:
        // 200 with any body (even empty list) confirms the query ran without schema errors
        response.statusCode() == 200
    }

    def "GET /products/{code} executes without schema errors"() {
        when:
        def response = get("/products/W1")

        then:
        // 200 or 404 are both valid — we only care that no schema error (500) occurred
        response.statusCode() in [200, 404]
    }

    def "POST /quote executes without schema errors"() {
        when:
        def response = post("/quote", '{"productCodeQuantities":{"W1":1}}')

        then:
        // 200 or 400 are both valid — 400 means W1 not seeded yet, which is fine
        // 500 would indicate a schema error (column missing, etc.)
        response.statusCode() in [200, 400]
    }
}
