package vanilla.controller

import groovy.json.JsonSlurper
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import org.springframework.boot.test.web.server.LocalServerPort
import spock.lang.Shared
import vanilla.AbstractIntegrationSpec

class ProductControllerSpec extends AbstractIntegrationSpec {

    @LocalServerPort
    int port

    @Shared
    HttpClient httpClient = HttpClient.newHttpClient()

    private HttpResponse<String> get(String path) {
        def request = HttpRequest.newBuilder(URI.create("http://localhost:${port}${path}")).GET().build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private HttpResponse<String> post(String path, String json) {
        def request = HttpRequest.newBuilder(URI.create("http://localhost:${port}${path}"))
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .header("Content-Type", "application/json")
            .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private HttpResponse<String> put(String path, String json) {
        def request = HttpRequest.newBuilder(URI.create("http://localhost:${port}${path}"))
            .PUT(HttpRequest.BodyPublishers.ofString(json))
            .header("Content-Type", "application/json")
            .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private HttpResponse<String> delete(String path) {
        def request = HttpRequest.newBuilder(URI.create("http://localhost:${port}${path}"))
            .DELETE()
            .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    def "GET /products/{code} returns 200 and product body for a known code"() {
        when:
        def response = get("/products/W1")
        def body = new JsonSlurper().parseText(response.body())

        then:
        response.statusCode() == 200
        body.code == 'W1'
        body.priceCents == 123
    }

    def "GET /products/{code} returns 404 for an unknown code"() {
        when:
        def response = get("/products/UNKNOWN")

        then:
        response.statusCode() == 404
    }

    def "POST /products creates a product and returns 201 with Location header"() {
        when:
        def response = post("/products", '{"code":"T1","name":"Test Widget","priceCents":500}')
        def body = new JsonSlurper().parseText(response.body())

        then:
        response.statusCode() == 201
        response.headers().firstValue("Location").get().endsWith("/products/T1")
        body.code == 'T1'
        body.priceCents == 500

        cleanup:
        delete("/products/T1")
    }

    def "POST /products returns 409 when code already exists"() {
        when:
        def response = post("/products", '{"code":"W1","name":"Duplicate","priceCents":0}')

        then:
        response.statusCode() == 409
    }

    def "PUT /products/{code} updates a product and returns 200"() {
        when:
        def response = put("/products/W2", '{"name":"Widget Two Updated","priceCents":999}')
        def body = new JsonSlurper().parseText(response.body())

        then:
        response.statusCode() == 200
        body.name == 'Widget Two Updated'
        body.priceCents == 999

        cleanup:
        put("/products/W2", '{"name":"Widget 2","priceCents":234}')
    }

    def "PUT /products/{code} returns 404 for an unknown code"() {
        when:
        def response = put("/products/UNKNOWN", '{"name":"X","priceCents":0}')

        then:
        response.statusCode() == 404
    }

    def "DELETE /products/{code} deletes a product and returns 204"() {
        given:
        post("/products", '{"code":"T2","name":"To Delete","priceCents":100}')

        when:
        def response = delete("/products/T2")

        then:
        response.statusCode() == 204
    }

    def "DELETE /products/{code} returns 404 for an unknown code"() {
        when:
        def response = delete("/products/UNKNOWN")

        then:
        response.statusCode() == 404
    }
}
