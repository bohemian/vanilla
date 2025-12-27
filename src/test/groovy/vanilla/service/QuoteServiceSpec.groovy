package vanilla.service

import vanilla.model.ProductPrice
import vanilla.model.ProductPriceRepository
import vanilla.model.QuoteParams
import spock.lang.Specification
import spock.lang.Unroll

import static vanilla.model.Product.BATTERY
import static vanilla.model.Product.SOLAR_PANEL

class QuoteServiceSpec extends Specification {

    @Unroll
    def "quote(#quantities) should be #expected"() {
        given:
        def repo = Mock(ProductPriceRepository)
        repo.findById("BATTERY") >> Optional.of(new ProductPrice([product: "BATTERY", costCents: 1111]))
        repo.findById("SOLAR_PANEL") >> Optional.of(new ProductPrice([product: "SOLAR_PANEL", costCents: 2222]))
        def service = new QuoteService(repo)
        def params = new QuoteParams(quantities)

        when:
        def actual = service.getQuote(params)

        then:
        actual.getTotalCents() == expected

        where:
        quantities                       | expected
        [(BATTERY): 3, (SOLAR_PANEL): 4] | 1111 * 3 + 2222 * 4
    }
}