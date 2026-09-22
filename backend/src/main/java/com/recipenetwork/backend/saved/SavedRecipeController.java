package com.recipenetwork.backend.saved;

import com.recipenetwork.backend.auth.CurrentUserResolver;
import com.recipenetwork.backend.auth.User;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SavedRecipeController {

    private final SavedRecipeService savedRecipeService;
    private final CurrentUserResolver currentUserResolver;

    public SavedRecipeController(SavedRecipeService savedRecipeService, CurrentUserResolver currentUserResolver) {
        this.savedRecipeService = savedRecipeService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/api/recipes/{id}/save")
    public ResponseEntity<SavedRecipeResponse> save(@AuthenticationPrincipal OAuth2User principal, @PathVariable Long id) {
        User user = currentUserResolver.requireCurrentUser(principal);
        SavedRecipeService.SaveResult result = savedRecipeService.save(user, id);
        HttpStatus status = result.newlyCreated() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.savedRecipe());
    }

    @DeleteMapping("/api/recipes/{id}/save")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsave(@AuthenticationPrincipal OAuth2User principal, @PathVariable Long id) {
        User user = currentUserResolver.requireCurrentUser(principal);
        savedRecipeService.unsave(user, id);
    }

    @GetMapping("/api/saved-recipes")
    public List<SavedRecipeResponse> savedRecipes(@AuthenticationPrincipal OAuth2User principal) {
        User user = currentUserResolver.requireCurrentUser(principal);
        return savedRecipeService.findSaved(user);
    }
}
