package vanilla.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import vanilla.AbstractIntegrationSpec
import vanilla.entity.Product
import vanilla.repository.ProductRepository

class ProductServiceIntegrationSpec extends AbstractIntegrationSpec {

    @Autowired
    ProductService productService

    @Autowired
    ProductRepository productRepository

    @Autowired
    JdbcTemplate jdbcTemplate

    def "getActiveProductCodes returns all seeded products when maxPriceCents covers all prices"() {
        when:
        def result = productService.getActiveProductCodes(10000)

        then:
        result.containsAll(['W1', 'W2', 'W3'])
    }

    def "getActiveProductCodes excludes products above max price"() {
        given:
        productRepository.save(new Product(code: 'EXPENSIVE', name: 'Expensive Widget', priceCents: 99999))

        when:
        def result = productService.getActiveProductCodes(500)

        then:
        !result.contains('EXPENSIVE')

        cleanup:
        // Use native SQL to avoid Hibernate loading ProductPriceHistory (entity has schema mismatch on 'name' column)
        jdbcTemplate.update("DELETE FROM product_price_history WHERE product_id = (SELECT id FROM product WHERE code = 'EXPENSIVE')")
        jdbcTemplate.update("DELETE FROM product WHERE code = 'EXPENSIVE'")
    }
}
