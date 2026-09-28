package com.recipenetwork.backend.auth;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end coverage of the security filter chain itself (CORS, CSRF, OAuth2 login
 * wiring, shared error shape for filter-level rejections) - complementary to the
 * per-controller @WebMvcTest classes, which run with security filters disabled and
 * only assert business-logic status codes/JSON contracts.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousRequestToProtectedEndpointReturns401WithSharedErrorShape() throws Exception {
        mockMvc.perform(get("/api/feed"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/feed"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void everyRequestSetsXsrfTokenCookieEvenOnPermitAllPaths() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void corsPreflightAllowsConfiguredFrontendOrigin() throws Exception {
        mockMvc.perform(options("/api/feed")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void corsPreflightRejectsDisallowedOrigin() throws Exception {
        mockMvc.perform(options("/api/feed")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void stateChangingRequestWithoutCsrfTokenIsRejectedWithSharedErrorShape() throws Exception {
        // /api/auth/logout is permitAll for *authorization*, but CSRF protection is a
        // separate, always-on concern - no path is exempted from it in SecurityConfig.
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/api/auth/logout"));
    }

    @Test
    void stateChangingRequestWithValidCsrfTokenIsAccepted() throws Exception {
        mockMvc.perform(post("/api/auth/logout").with(csrf())).andExpect(status().isNoContent());
    }

    @Test
    void oauth2LoginRedirectsToGoogleAuthorizationEndpoint() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("accounts.google.com")));
    }
}
