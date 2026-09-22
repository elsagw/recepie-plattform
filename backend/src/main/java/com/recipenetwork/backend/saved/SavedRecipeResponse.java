package com.recipenetwork.backend.saved;

import java.time.OffsetDateTime;

public record SavedRecipeResponse(Long recipeId, String title, String imageUrl, String domain, OffsetDateTime savedAt) {

    public static SavedRecipeResponse from(SavedRecipe savedRecipe) {
        return new SavedRecipeResponse(
                savedRecipe.getRecipe().getId(),
                savedRecipe.getRecipe().getTitle(),
                savedRecipe.getRecipe().getImageUrl(),
                savedRecipe.getRecipe().getDomain(),
                savedRecipe.getCreatedAt());
    }
}
