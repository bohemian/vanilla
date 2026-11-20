package vanilla.health;

import javax.sql.DataSource;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

/**
 * Verifies database connectivity and that the product table is populated. Goes beyond the built-in
 * {@code db} indicator (SELECT 1) by confirming the application schema is reachable and seeded.
 */
@Component
public class DatabaseHealthIndicator extends AbstractHealthIndicator {

  private final DataSource dataSource;

  public DatabaseHealthIndicator(DataSource dataSource) {
    super("Database health check failed");
    this.dataSource = dataSource;
  }

  @Override
  protected void doHealthCheck(Health.Builder builder) throws Exception {
    try (var conn = dataSource.getConnection();
        var stmt = conn.createStatement();
        var rs = stmt.executeQuery("SELECT COUNT(*) FROM product")) {
      if (rs.next()) {
        long productCount = rs.getLong(1);
        if (productCount > 0) {
          builder.up().withDetail("productCount", productCount);
        } else {
          builder.down().withDetail("reason", "product table is empty");
        }
      } else {
        builder.down().withDetail("reason", "COUNT(*) returned no rows");
      }
    }
  }
}
