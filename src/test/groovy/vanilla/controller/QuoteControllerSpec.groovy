package vanilla.controller

import groovy.json.JsonSlurper
import org.springframework.boot.test.web.server.LocalServerPort
import spock.lang.Shared
import vanilla.AbstractIntegrationSpec

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class QuoteControllerSpec extends AbstractIntegrationSpec {

    @LocalServerPort
    int port

    @Shared
    HttpClient httpClient = HttpClient.newHttpClient()

    private HttpResponse<String> post(String path, String body) {
        def request = HttpRequest.newBuilder(URI.create("http://localhost:${port}${path}"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    def "POST /quote returns total cents for known product"() {
        when:
        def response = post("/quote", '{"productCodeQuantities":{"W1":2}}')
        def body = new JsonSlurper().parseText(response.body())

        then:
        response.statusCode() == 200
        body.totalCents == 246
    }

    def "POST /quote returns 400 for unknown product codes"() {
        when:
        def response = post("/quote", '{"productCodeQuantities":{"UNKNOWN":1}}')

        then:
        response.statusCode() == 400
    }
}
