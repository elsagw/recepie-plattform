package com.recipenetwork.backend.auth;

import com.recipenetwork.backend.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Returns the shared JSON error shape for /api/** access-denied cases - most commonly an
 * authenticated request with a missing/invalid CSRF token - instead of Spring Boot's
 * generic {timestamp, status, error, path} error page. Mirrors ApiAuthenticationEntryPoint,
 * which only covers the anonymous-caller case; this one covers the authenticated-but-denied case.
 */
@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public ApiAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws java.io.IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(), "FORBIDDEN", "Åtkomst nekad.",
                request.getRequestURI(), OffsetDateTime.now());
        // See ApiAuthenticationEntryPoint: getOutputStream(), not getWriter(), so this
        // always writes UTF-8 regardless of the response's default character encoding.
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
