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
}
