package vanilla

import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import vanilla.dto.QuoteParams
import vanilla.service.QuoteService

class DatabaseSpec extends AbstractIntegrationSpec {

    @Autowired
    QuoteService quoteService

    @Unroll
    def "quote(#quantities) should be #expected"() {
        given:
        assert quoteService != null: "quoteService not injected"
        def params = new QuoteParams(quantities)

        when:
        def actual = quoteService.getQuote(params)

        then:
        actual.getTotalCents() == expected

        where:
        quantities                  | expected
        ['W1': 3]                   | 369L
        ['W1': 3, 'W2': 4, 'W3': 5] | 123L * 3 + 234L * 4 + 345L * 5
    }

    @Unroll
    def "price history should be saved"() {
        given:
        assert quoteService != null: "quoteService not injected"
        def params = new QuoteParams(quantities)

        when:
        def actual = quoteService.getQuote(params)

        then:
        actual.getTotalCents() == expected

        where:
        quantities | expected
        ['W1': 3]  | 369L
    }
}
