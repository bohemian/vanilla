package vanilla.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Manually wires Flyway into Spring Boot 4.x, which no longer includes FlywayAutoConfiguration. */
@Configuration
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", matchIfMissing = true)
public class FlywayConfig {

  @Bean
  public Flyway flyway(DataSource dataSource) {
    Flyway flyway =
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load();
    flyway.migrate();
    return flyway;
  }
}
