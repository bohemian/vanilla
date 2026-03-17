package vanilla.service

import spock.lang.Specification
import spock.lang.Unroll
import vanilla.dto.QuoteParams
import vanilla.entity.Product
import vanilla.repository.ProductRepository
//inport vanilla.TestUtils

class QuoteServiceComponentSpec extends Specification {

    @Unroll
    def "quote(#quantities) should be #expected"() {
        given:
        def repo = Mock(ProductRepository)
        repo.findAllByCodeIn({it.containsAll(['W1', 'W2'])}) >> [
            new Product([code: "W1", priceCents: 1111]),
            new Product([code: "W2", priceCents: 2222])
        ]

        def service = new QuoteService(repo)
        def params = new QuoteParams(quantities)

        when:
        def actual = service.getQuote(params)

        then:
        actual.getTotalCents() == expected

        where:
        quantities         | expected
        ['W1': 3, 'W2': 4] | 1111 * 3 + 2222 * 4
    }

}