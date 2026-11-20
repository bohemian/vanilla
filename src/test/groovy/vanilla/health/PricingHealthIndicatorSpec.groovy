package vanilla.health

import org.springframework.boot.health.contributor.Status
import spock.lang.Specification
import vanilla.dto.Quote
import vanilla.dto.QuoteParams
import vanilla.service.QuoteService

class PricingHealthIndicatorSpec extends Specification {

    QuoteService quoteService = Mock()
    PricingHealthIndicator indicator = new PricingHealthIndicator(quoteService)

    def "reports UP with price when HEALTH product returns a quote"() {
        given:
        quoteService.getQuote(_ as QuoteParams) >> new Quote(999900L)

        when:
        def health = indicator.health()

        then:
        health.status == Status.UP
        health.details.healthProductPriceCents == 999900L
    }

    def "reports DOWN when getQuote returns null"() {
        given:
        quoteService.getQuote(_ as QuoteParams) >> null

        when:
        def health = indicator.health()

        then:
        health.status == Status.DOWN
        health.details.reason =~ /null/
    }

    def "reports DOWN when getQuote throws"() {
        given:
        quoteService.getQuote(_ as QuoteParams) >> { throw new IllegalArgumentException("Invalid product codes: [HEALTH]") }

        when:
        def health = indicator.health()

        then:
        health.status == Status.DOWN
    }

    def "passes HEALTH product code to quote service"() {
        given:
        quoteService.getQuote(_ as QuoteParams) >> new Quote(1L)

        when:
        indicator.health()

        then:
        1 * quoteService.getQuote({ QuoteParams p ->
            p.productCodeQuantities == [(PricingHealthIndicator.HEALTH_PRODUCT_CODE): 1]
        })
    }
}
