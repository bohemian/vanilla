package vanilla.utils

import spock.lang.Specification

class CacheSpec extends Specification {

    def "computes value on first access"() {
        given:
        def cache = new Cache<String, Integer>({ k -> k.length() })

        expect:
        cache.get("hello") == 5
    }

    def "returns cached value on subsequent access without recomputing"() {
        given:
        int computations = 0
        def cache = new Cache<String, Integer>({ k -> computations++; k.length() })

        when:
        cache.get("hello")
        cache.get("hello")
        cache.get("hello")

        then:
        computations == 1
    }

    def "computes separate values for different keys"() {
        given:
        def cache = new Cache<String, Integer>({ k -> k.length() })

        expect:
        cache.get("hi") == 2
        cache.get("hello") == 5
    }
}
