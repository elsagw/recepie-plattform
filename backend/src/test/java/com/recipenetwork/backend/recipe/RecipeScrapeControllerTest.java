package com.recipenetwork.backend.recipe;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recipenetwork.backend.common.ApiException;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RecipeScrapeController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecipeScrapeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecipeScrapeService recipeScrapeService;

    @Test
    void scrapeReturns200WithRecipePreview() throws Exception {
        ExternalRecipe recipe = new ExternalRecipe(
                "https://example.com/recept", "Exempelrecept", "https://cdn.example/img.jpg",
                "example.com", OffsetDateTime.now());
        when(recipeScrapeService.scrape("https://example.com/recept")).thenReturn(recipe);

        mockMvc.perform(post("/api/recipes/scrape").param("url", "https://example.com/recept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Exempelrecept"))
                .andExpect(jsonPath("$.domain").value("example.com"));
    }

    @Test
    void scrapeReturns400ForInvalidUrl() throws Exception {
        when(recipeScrapeService.scrape("not-a-url"))
                .thenThrow(new ApiException(HttpStatus.BAD_REQUEST, "INVALID_URL", "Ogiltig URL."));

        mockMvc.perform(post("/api/recipes/scrape").param("url", "not-a-url"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_URL"));
    }

    @Test
    void scrapeReturns502WhenExternalSiteUnreachable() throws Exception {
        when(recipeScrapeService.scrape("https://unreachable.example/"))
                .thenThrow(new ApiException(HttpStatus.BAD_GATEWAY, "FETCH_FAILED", "Kunde inte hämta receptet."));

        mockMvc.perform(post("/api/recipes/scrape").param("url", "https://unreachable.example/"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("FETCH_FAILED"));
    }

    @Test
    void scrapeReturns400WhenUrlParamMissing() throws Exception {
        mockMvc.perform(post("/api/recipes/scrape"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"));
    }
}
