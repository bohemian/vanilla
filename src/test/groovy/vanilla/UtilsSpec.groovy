package vanilla

import spock.lang.Specification
import spock.lang.Unroll

import static java.time.DayOfWeek.MONDAY
import static vanilla.Utils.toSentenceCase

class UtilsSpec extends Specification {

    @Unroll
    def "Test toSentenceCase"() {
        expect:
        toSentenceCase(val) == expected

        where:
        val    | expected
        MONDAY | 'Monday'
    }
}
