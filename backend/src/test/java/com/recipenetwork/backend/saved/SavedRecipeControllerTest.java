package com.recipenetwork.backend.saved;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recipenetwork.backend.auth.CurrentUserResolver;
import com.recipenetwork.backend.auth.User;
import com.recipenetwork.backend.common.ApiException;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SavedRecipeController.class)
@AutoConfigureMockMvc(addFilters = false)
class SavedRecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SavedRecipeService savedRecipeService;

    @MockitoBean
    private CurrentUserResolver currentUserResolver;

    private final User user = new User("google-sub-1", "elsa@example.com", "Elsa");

    {
        ReflectionTestUtils.setField(user, "id", 7L);
    }

    private static final SavedRecipeResponse SAVED_RECIPE_RESPONSE = new SavedRecipeResponse(
            12L, "Exempelrecept", null, "example.com", "https://example.com/", OffsetDateTime.now());

    @Test
    void saveReturns201WhenNewlyCreated() throws Exception {
        when(currentUserResolver.requireCurrentUser(any())).thenReturn(user);
        when(savedRecipeService.save(eq(user), eq(12L)))
                .thenReturn(new SavedRecipeService.SaveResult(SAVED_RECIPE_RESPONSE, true));

        mockMvc.perform(post("/api/recipes/12/save"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.recipeId").value(12));
    }

    @Test
    void saveReturns200WhenAlreadySaved() throws Exception {
        when(currentUserResolver.requireCurrentUser(any())).thenReturn(user);
        when(savedRecipeService.save(eq(user), eq(12L)))
                .thenReturn(new SavedRecipeService.SaveResult(SAVED_RECIPE_RESPONSE, false));

        mockMvc.perform(post("/api/recipes/12/save")).andExpect(status().isOk());
    }

    @Test
    void saveReturns404WhenRecipeMissing() throws Exception {
        when(currentUserResolver.requireCurrentUser(any())).thenReturn(user);
        when(savedRecipeService.save(eq(user), eq(99L)))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "RECIPE_NOT_FOUND", "Receptet hittades inte."));

        mockMvc.perform(post("/api/recipes/99/save")).andExpect(status().isNotFound());
    }

    @Test
    void saveReturns401WhenNotLoggedIn() throws Exception {
        when(currentUserResolver.requireCurrentUser(any()))
                .thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Du måste vara inloggad."));

        mockMvc.perform(post("/api/recipes/12/save")).andExpect(status().isUnauthorized());
    }

    @Test
    void unsaveReturns204RegardlessOfPriorState() throws Exception {
        when(currentUserResolver.requireCurrentUser(any())).thenReturn(user);

        mockMvc.perform(delete("/api/recipes/12/save")).andExpect(status().isNoContent());
    }

    @Test
    void savedRecipesReturnsList() throws Exception {
        when(currentUserResolver.requireCurrentUser(any())).thenReturn(user);
        when(savedRecipeService.findSaved(user)).thenReturn(List.of(SAVED_RECIPE_RESPONSE));

        mockMvc.perform(get("/api/saved-recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].domain").value("example.com"));
    }

    @Test
    void savedRecipesReturnsEmptyListWhenNoneSaved() throws Exception {
        when(currentUserResolver.requireCurrentUser(any())).thenReturn(user);
        when(savedRecipeService.findSaved(user)).thenReturn(List.of());

        mockMvc.perform(get("/api/saved-recipes"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }
}
