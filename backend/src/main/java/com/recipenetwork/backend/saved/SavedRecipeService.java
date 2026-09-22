package com.recipenetwork.backend.saved;

import com.recipenetwork.backend.auth.User;
import com.recipenetwork.backend.common.ApiException;
import com.recipenetwork.backend.recipe.ExternalRecipe;
import com.recipenetwork.backend.recipe.ExternalRecipeRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SavedRecipeService {

    private final SavedRecipeRepository savedRecipeRepository;
    private final ExternalRecipeRepository externalRecipeRepository;

    public SavedRecipeService(
            SavedRecipeRepository savedRecipeRepository, ExternalRecipeRepository externalRecipeRepository) {
        this.savedRecipeRepository = savedRecipeRepository;
        this.externalRecipeRepository = externalRecipeRepository;
    }

    public record SaveResult(SavedRecipeResponse savedRecipe, boolean newlyCreated) {
    }

    @Transactional
    public SaveResult save(User user, Long recipeId) {
        return savedRecipeRepository.findByUser_IdAndRecipe_Id(user.getId(), recipeId)
                .map(existing -> new SaveResult(SavedRecipeResponse.from(existing), false))
                .orElseGet(() -> createSaved(user, recipeId));
    }

    private SaveResult createSaved(User user, Long recipeId) {
        ExternalRecipe recipe = externalRecipeRepository.findById(recipeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RECIPE_NOT_FOUND", "Receptet hittades inte."));

        try {
            SavedRecipe saved = savedRecipeRepository.save(new SavedRecipe(user, recipe, OffsetDateTime.now()));
            return new SaveResult(SavedRecipeResponse.from(saved), true);
        } catch (DataIntegrityViolationException e) {
            // Lost a race with a concurrent save of the same recipe by the same user.
            SavedRecipe existing = savedRecipeRepository.findByUser_IdAndRecipe_Id(user.getId(), recipeId)
                    .orElseThrow(() -> e);
            return new SaveResult(SavedRecipeResponse.from(existing), false);
        }
    }

    @Transactional
    public void unsave(User user, Long recipeId) {
        savedRecipeRepository.deleteByUser_IdAndRecipe_Id(user.getId(), recipeId);
    }

    @Transactional(readOnly = true)
    public List<SavedRecipeResponse> findSaved(User user) {
        return savedRecipeRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(SavedRecipeResponse::from)
                .toList();
    }
}
