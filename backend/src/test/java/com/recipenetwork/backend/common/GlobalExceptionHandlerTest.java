package com.recipenetwork.backend.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Direct coverage of docs/ARCHITECTURE.md's "every error status maps to the shared JSON
 * shape" rule (400/401/404/409/422/502, plus the framework-exception and unexpected-error
 * fallbacks) - complementary to the per-controller tests, which each only exercise the
 * specific codes that controller can actually produce.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @ParameterizedTest
    @EnumSource(
            value = HttpStatus.class,
            names = {"BAD_REQUEST", "UNAUTHORIZED", "NOT_FOUND", "CONFLICT", "UNPROCESSABLE_ENTITY", "BAD_GATEWAY"})
    void apiExceptionMapsToSharedShapeForEveryStatus(HttpStatus status) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/example");
        ApiException ex = new ApiException(status, "SOME_CODE", "Ett användarvänligt meddelande.");

        var response = handler.handleApiException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody().status()).isEqualTo(status.value());
        assertThat(response.getBody().code()).isEqualTo("SOME_CODE");
        assertThat(response.getBody().message()).isEqualTo("Ett användarvänligt meddelande.");
        assertThat(response.getBody().path()).isEqualTo("/api/example");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    void missingParameterMapsTo400WithParameterNameInMessage() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/recipes/scrape");
        var ex = new MissingServletRequestParameterException("url", "String");

        var response = handler.handleMissingParameter(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code()).isEqualTo("MISSING_PARAMETER");
        assertThat(response.getBody().message()).contains("url");
    }

    @Test
    void noResourceFoundMapsTo404WithSafeMessage() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/does-not-exist");
        var ex = new NoResourceFoundException(
                org.springframework.http.HttpMethod.GET, "/api/does-not-exist", "no route found");

        var response = handler.handleNoResourceFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
    }

    @Test
    void frameworkErrorResponseExceptionHonorsItsOwnStatusButMasksItsMessage() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/recipes/scrape");
        var ex = new HttpRequestMethodNotSupportedException("PATCH");

        var response = handler.handleUnexpected(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody().code()).isEqualTo("REQUEST_ERROR");
        assertThat(response.getBody().message()).isEqualTo("Kunde inte hantera förfrågan.");
    }

    @Test
    void unexpectedExceptionMapsTo500AndNeverLeaksRawExceptionMessage() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/feed");
        var ex = new RuntimeException("Connection refused: could not connect to db-host:5432");

        var response = handler.handleUnexpected(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).doesNotContain("db-host").doesNotContain("Connection refused");
    }
}
