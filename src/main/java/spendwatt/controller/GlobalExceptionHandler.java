package spendwatt.controller;

import org.springframework.http.HttpStatus;
import static org.springframework.http.HttpStatus.*;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;

/**
 * RFC 9457 compliant
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc9457">RFC 9457</a>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        return problemDetail(BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        return problemDetail(CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex, WebRequest request) {
        return problemDetail(ex);
    }

    private static ProblemDetail problemDetail(Exception exception) {
        ProblemDetail problemDetail = problemDetail(INTERNAL_SERVER_ERROR, exception.getMessage());
        problemDetail.setProperty("exception", exception.getClass().getSimpleName());
        return problemDetail;
    }

    private static ProblemDetail problemDetail(HttpStatus httpStatus, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(httpStatus, detail);
        problemDetail.setTitle(httpStatus.getReasonPhrase());
        problemDetail.setType(URI.create("https://en.wikipedia.org/wiki/List_of_HTTP_status_codes#" + httpStatus.value()));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

}
