package vanilla.controller

import org.springframework.http.HttpStatus
import spock.lang.Specification

class GlobalExceptionHandlerSpec extends Specification {

    GlobalExceptionHandler handler = new GlobalExceptionHandler()

    def "handleIllegalArgumentException returns 400 with message"() {
        when:
        def result = handler.handleIllegalArgumentException(new IllegalArgumentException("bad input"), null)

        then:
        result.status == HttpStatus.BAD_REQUEST.value()
        result.detail == "bad input"
    }

    def "handleIllegalStateException returns 409 with message"() {
        when:
        def result = handler.handleIllegalStateException(new IllegalStateException("conflict"), null)

        then:
        result.status == HttpStatus.CONFLICT.value()
        result.detail == "conflict"
    }

    def "handleGenericException returns 500 with exception class name"() {
        when:
        def result = handler.handleGenericException(new RuntimeException("boom"), null)

        then:
        result.status == HttpStatus.INTERNAL_SERVER_ERROR.value()
        result.properties["exception"] == "RuntimeException"
    }

    def "problem detail includes timestamp property"() {
        when:
        def result = handler.handleIllegalArgumentException(new IllegalArgumentException("x"), null)

        then:
        result.properties["timestamp"] != null
    }
}
