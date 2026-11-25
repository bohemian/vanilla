package vanilla.blackbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jbehave.core.annotations.AfterScenario;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

public class ProductCrudSteps {

  private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
  private static final HttpClient HTTP = HttpClient.newHttpClient();
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private HttpResponse<String> lastResponse;
  private final List<String> createdCodes = new ArrayList<>();

  @AfterScenario
  public void cleanup() throws Exception {
    for (String code : createdCodes) {
      HTTP.send(
          HttpRequest.newBuilder().DELETE().uri(URI.create(BASE_URL + "/products/" + code)).build(),
          HttpResponse.BodyHandlers.ofString());
    }
    createdCodes.clear();
  }

  @Given("no product with code $code exists")
  public void noProductExists(String code) throws Exception {
    HTTP.send(
        HttpRequest.newBuilder().DELETE().uri(URI.create(BASE_URL + "/products/" + code)).build(),
        HttpResponse.BodyHandlers.ofString());
  }

  @Given("a product with code $code name $name price $priceCents exists")
  public void productExists(String code, String name, int priceCents) throws Exception {
    String body =
        MAPPER.writeValueAsString(Map.of("code", code, "name", name, "priceCents", priceCents));
    HttpResponse<String> resp =
        HTTP.send(
            HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .uri(URI.create(BASE_URL + "/products"))
                .header("Content-Type", "application/json")
                .build(),
            HttpResponse.BodyHandlers.ofString());
    assertTrue(
        resp.statusCode() == 201 || resp.statusCode() == 409,
        "Setup failed with status " + resp.statusCode());
    createdCodes.add(code);
  }

  @When("I create a product with code $code name $name and price $priceCents")
  public void createProduct(String code, String name, int priceCents) throws Exception {
    String body =
        MAPPER.writeValueAsString(Map.of("code", code, "name", name, "priceCents", priceCents));
    lastResponse =
        HTTP.send(
            HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .uri(URI.create(BASE_URL + "/products"))
                .header("Content-Type", "application/json")
                .build(),
            HttpResponse.BodyHandlers.ofString());
    if (lastResponse.statusCode() == 201) {
      createdCodes.add(code);
    }
  }

  @When("I update product $code with name $name and price $priceCents")
  public void updateProduct(String code, String name, int priceCents) throws Exception {
    String body =
        MAPPER.writeValueAsString(Map.of("code", code, "name", name, "priceCents", priceCents));
    lastResponse =
        HTTP.send(
            HttpRequest.newBuilder()
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .uri(URI.create(BASE_URL + "/products/" + code))
                .header("Content-Type", "application/json")
                .build(),
            HttpResponse.BodyHandlers.ofString());
  }

  @When("I delete product $code")
  public void deleteProduct(String code) throws Exception {
    lastResponse =
        HTTP.send(
            HttpRequest.newBuilder()
                .DELETE()
                .uri(URI.create(BASE_URL + "/products/" + code))
                .build(),
            HttpResponse.BodyHandlers.ofString());
    createdCodes.remove(code);
  }

  @Then("the response status is $status")
  public void responseStatusIs(int status) {
    assertNotNull(lastResponse, "No HTTP response recorded — missing @When step before this @Then");
    assertEquals(status, lastResponse.statusCode(), "Response body: " + lastResponse.body());
  }

  @Then("the response contains a product with code $code")
  public void responseContainsProduct(String code) throws Exception {
    assertNotNull(lastResponse, "No HTTP response recorded — missing @When step before this @Then");
    assertNotNull(lastResponse.body(), "Response body is null");
    JsonNode json = MAPPER.readTree(lastResponse.body());
    JsonNode codeNode = json.get("code");
    assertNotNull(codeNode, "Missing 'code' field in response: " + lastResponse.body());
    assertEquals(code, codeNode.asText());
  }
}
