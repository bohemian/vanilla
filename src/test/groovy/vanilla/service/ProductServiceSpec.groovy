package vanilla.service

import spock.lang.Specification
import vanilla.entity.Product
import vanilla.repository.ProductRepository

class ProductServiceSpec extends Specification {

    def productRepository = Mock(ProductRepository)
    def service = new ProductService(productRepository)

    def "getActiveProductCodes returns codes for products at or below max price"() {
        given:
        productRepository.findAll() >> [
            new Product(code: 'A', priceCents: 100),
            new Product(code: 'B', priceCents: 200),
            new Product(code: 'C', priceCents: 50),
        ]

        when:
        def result = service.getActiveProductCodes(100)

        then:
        result == ['A', 'C']
    }

    def "getActiveProductCodes returns empty list when all products exceed max price"() {
        given:
        productRepository.findAll() >> [
            new Product(code: 'A', priceCents: 200),
        ]

        when:
        def result = service.getActiveProductCodes(100)

        then:
        result == []
    }
}
