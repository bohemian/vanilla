package vanilla.service

import spock.lang.Specification
import vanilla.entity.Product
import vanilla.repository.ProductRepository

class ProductServiceSpec extends Specification {

    def productRepository = Mock(ProductRepository)
    def service = new ProductService(productRepository)

    def "getActiveProductCodes returns codes for products with non-negative price"() {
        given:
        productRepository.findAll() >> [
            new Product(code: 'A', priceCents: 100),
            new Product(code: 'B', priceCents: 0),
            new Product(code: 'C', priceCents: 50),
        ]

        when:
        def result = service.getActiveProductCodes()

        then:
        result == ['A', 'B', 'C']
    }

    def "getActiveProductCodes excludes products with negative price"() {
        given:
        productRepository.findAll() >> [
            new Product(code: 'A', priceCents: -1),
        ]

        when:
        def result = service.getActiveProductCodes()

        then:
        result == []
    }
}
