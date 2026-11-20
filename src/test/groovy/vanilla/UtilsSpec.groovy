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

    def "encodeJWT and decodeJWT round-trip"() {
        given:
        def header = '{"alg":"HS256","typ":"JWT"}'
        def payload = '{"sub":"user1","iat":1700000000}'
        def sig = 'fakesignature'

        when:
        def encoded = Utils.encodeJWT(header, payload, sig)
        def parts = Utils.decodeJWT(encoded)

        then:
        parts[0] == header
        parts[1] == payload
        parts[2] == sig
    }

    def "decodeJWT throws RuntimeException for token without three parts"() {
        when:
        Utils.decodeJWT("onlytwoparts.here")

        then:
        thrown(RuntimeException)
    }

    def "encodeJWT produces three dot-separated parts"() {
        when:
        def result = Utils.encodeJWT('{"a":1}', '{"b":2}', 'sig')

        then:
        result.split('\\.').length == 3
    }
}
