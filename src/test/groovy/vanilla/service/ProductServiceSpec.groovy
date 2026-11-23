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

    def "getAllProducts delegates to repository findAll"() {
        given:
        productRepository.findAll() >> [
            new Product(code: 'A', priceCents: 100),
            new Product(code: 'B', priceCents: 200),
        ]

        when:
        def result = service.getAllProducts()

        then:
        result.size() == 2
        result*.code == ['A', 'B']
    }

    def "createProduct saves product with correct fields and returns it"() {
        given:
        def saved = new Product(code: 'NEW', name: 'New Widget', priceCents: 500)
        productRepository.save(_ as Product) >> saved

        when:
        def result = service.createProduct('NEW', 'New Widget', 500)

        then:
        1 * productRepository.save({ it.code == 'NEW' && it.name == 'New Widget' && it.priceCents == 500 }) >> saved
        result.code == 'NEW'
        result.name == 'New Widget'
        result.priceCents == 500
    }

    def "updateProduct updates name and price when code found"() {
        given:
        def product = new Product(code: 'W1', name: 'Old Name', priceCents: 100)
        productRepository.findByCode('W1') >> Optional.of(product)
        productRepository.save(product) >> product

        when:
        def result = service.updateProduct('W1', 'New Name', 999)

        then:
        result.isPresent()
        result.get().name == 'New Name'
        result.get().priceCents == 999
    }

    def "updateProduct returns empty and does not save when code not found"() {
        given:
        productRepository.findByCode('UNKNOWN') >> Optional.empty()

        when:
        def result = service.updateProduct('UNKNOWN', 'X', 0)

        then:
        result.isEmpty()
        0 * productRepository.save(_)
    }

    def "deleteProduct deletes history then product and returns true when found"() {
        given:
        productRepository.deleteByCodeNative('W1') >> 1

        when:
        def result = service.deleteProduct('W1')

        then:
        1 * productRepository.deleteHistoryByProductCode('W1')
        result == true
    }

    def "deleteProduct returns false when code not found"() {
        given:
        productRepository.deleteByCodeNative('UNKNOWN') >> 0

        when:
        def result = service.deleteProduct('UNKNOWN')

        then:
        1 * productRepository.deleteHistoryByProductCode('UNKNOWN')
        result == false
    }
}
