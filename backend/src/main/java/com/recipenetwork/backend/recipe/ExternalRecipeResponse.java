package com.recipenetwork.backend.recipe;

import java.time.OffsetDateTime;

public record ExternalRecipeResponse(
        Long id, String sourceUrl, String title, String imageUrl, String domain, OffsetDateTime createdAt) {

    public static ExternalRecipeResponse from(ExternalRecipe recipe) {
        return new ExternalRecipeResponse(
                recipe.getId(), recipe.getSourceUrl(), recipe.getTitle(),
                recipe.getImageUrl(), recipe.getDomain(), recipe.getCreatedAt());
    }
}
