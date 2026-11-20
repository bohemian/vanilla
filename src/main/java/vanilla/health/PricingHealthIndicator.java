package vanilla.health;

import java.util.Map;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;
import vanilla.dto.QuoteParams;
import vanilla.service.QuoteService;

/**
 * Verifies the pricing pipeline end-to-end by requesting a quote for the sentinel {@code HEALTH}
 * product (inserted by V003 migration). A successful quote confirms that the product repository,
 * pricing logic, and database are all functioning correctly.
 */
@Component
public class PricingHealthIndicator extends AbstractHealthIndicator {

  static final String HEALTH_PRODUCT_CODE = "HEALTH";

  private final QuoteService quoteService;

  public PricingHealthIndicator(QuoteService quoteService) {
    super("Pricing health check failed");
    this.quoteService = quoteService;
  }

  @Override
  protected void doHealthCheck(Health.Builder builder) {
    var quote = quoteService.getQuote(new QuoteParams(Map.of(HEALTH_PRODUCT_CODE, 1)));
    if (quote == null) {
      builder.down().withDetail("reason", "getQuote returned null for HEALTH product");
    } else {
      builder.up().withDetail("healthProductPriceCents", quote.getTotalCents());
    }
  }
}
