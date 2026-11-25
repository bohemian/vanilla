package vanilla.blackbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

/**
 * Step definitions for the post-deploy verification suite.
 *
 * <p>All steps are read-only (GET requests only) and rely only on data that migrations guarantee to
 * exist (e.g. the HEALTH sentinel product seeded by V003). No side effects, no cleanup needed.
 */
public class VerificationSteps {

  private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
  private static final HttpClient HTTP = HttpClient.newHttpClient();
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private HttpResponse<String> lastResponse;

  @When("I check the readiness endpoint")
  public void checkReadiness() throws Exception {
    lastResponse =
        HTTP.send(
            HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE_URL + "/actuator/health/readiness"))
                .build(),
            HttpResponse.BodyHandlers.ofString());
  }

  @When("I fetch product with code $code")
  public void fetchProduct(String code) throws Exception {
    lastResponse =
        HTTP.send(
            HttpRequest.newBuilder().GET().uri(URI.create(BASE_URL + "/products/" + code)).build(),
            HttpResponse.BodyHandlers.ofString());
  }

  @Then("the health status is UP")
  public void healthStatusIsUp() throws Exception {
    assertNotNull(lastResponse, "No HTTP response recorded — missing @When step before this @Then");
    assertEquals(200, lastResponse.statusCode(), "Health endpoint body: " + lastResponse.body());
    JsonNode json = MAPPER.readTree(lastResponse.body());
    JsonNode statusNode = json.get("status");
    assertNotNull(statusNode, "No 'status' field in health response: " + lastResponse.body());
    assertEquals("UP", statusNode.asText(), "Health status: " + lastResponse.body());
  }

  @Then("the response status is $status")
  public void responseStatusIs(int status) {
    assertNotNull(lastResponse, "No HTTP response recorded — missing @When step before this @Then");
    assertEquals(status, lastResponse.statusCode(), "Response body: " + lastResponse.body());
  }

  @Then("the response contains a product with code $code")
  public void responseContainsProduct(String code) throws Exception {
    assertNotNull(lastResponse, "No HTTP response recorded — missing @When step before this @Then");
    JsonNode json = MAPPER.readTree(lastResponse.body());
    JsonNode codeNode = json.get("code");
    assertNotNull(codeNode, "Missing 'code' field in response: " + lastResponse.body());
    assertEquals(code, codeNode.asText(), "Wrong product code in: " + lastResponse.body());
  }
}
