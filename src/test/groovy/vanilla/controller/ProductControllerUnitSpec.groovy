package vanilla.controller

import org.springframework.http.HttpStatus
import spock.lang.Specification
import vanilla.entity.Product
import vanilla.service.ProductService

class ProductControllerUnitSpec extends Specification {

    def productService = Mock(ProductService)
    def controller = new ProductController(productService)

    def "POST /products returns 201 with Location header and body when product created"() {
        given:
        productService.getByCode('NEW') >> Optional.empty()
        productService.createProduct('NEW', 'New Widget', 500) >> new Product(code: 'NEW', name: 'New Widget', priceCents: 500)

        when:
        def response = controller.createProduct(new ProductRequest('NEW', 'New Widget', 500))

        then:
        response.statusCode.value() == 201
        response.headers.getFirst('Location') == '/products/NEW'
        response.body.code == 'NEW'
        response.body.priceCents == 500
    }

    def "POST /products returns 409 and does not create when code already exists"() {
        given:
        productService.getByCode('W1') >> Optional.of(new Product(code: 'W1'))

        when:
        def response = controller.createProduct(new ProductRequest('W1', 'Duplicate', 0))

        then:
        response.statusCode.value() == 409
        0 * productService.createProduct(_, _, _)
    }

    def "PUT /products/{code} returns 200 with updated product"() {
        given:
        def updated = new Product(code: 'W1', name: 'Updated Name', priceCents: 999)
        productService.updateProduct('W1', 'Updated Name', 999) >> Optional.of(updated)

        when:
        def response = controller.updateProduct('W1', new ProductRequest('W1', 'Updated Name', 999))

        then:
        response.statusCode.value() == 200
        response.body.name == 'Updated Name'
        response.body.priceCents == 999
    }

    def "PUT /products/{code} returns 404 when code not found"() {
        given:
        productService.updateProduct('UNKNOWN', 'X', 0) >> Optional.empty()

        when:
        def response = controller.updateProduct('UNKNOWN', new ProductRequest('UNKNOWN', 'X', 0))

        then:
        response.statusCode.value() == 404
    }

    def "DELETE /products/{code} returns 204 when product deleted"() {
        given:
        productService.deleteProduct('W1') >> true

        when:
        def response = controller.deleteProduct('W1')

        then:
        response.statusCode.value() == 204
    }

    def "DELETE /products/{code} returns 404 when product not found"() {
        given:
        productService.deleteProduct('UNKNOWN') >> false

        when:
        def response = controller.deleteProduct('UNKNOWN')

        then:
        response.statusCode.value() == 404
    }
}
