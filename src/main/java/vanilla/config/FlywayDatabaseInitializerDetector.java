package vanilla.config;

import java.util.Set;
import org.flywaydb.core.Flyway;
import org.springframework.boot.sql.init.dependency.AbstractBeansOfTypeDatabaseInitializerDetector;

/**
 * Registers the {@link Flyway} bean as a database initializer so that Spring Boot's JPA SPI can
 * make the EntityManagerFactory depend on it. This replaces the ordering that Spring Boot 3.x's
 * {@code FlywayAutoConfiguration} provided automatically.
 */
class FlywayDatabaseInitializerDetector extends AbstractBeansOfTypeDatabaseInitializerDetector {

  @Override
  protected Set<Class<?>> getDatabaseInitializerBeanTypes() {
    return Set.of(Flyway.class);
  }
}
