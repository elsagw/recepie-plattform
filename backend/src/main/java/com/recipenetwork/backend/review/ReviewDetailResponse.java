package com.recipenetwork.backend.review;

import com.recipenetwork.backend.recipe.ExternalRecipe;
import java.time.OffsetDateTime;

public record ReviewDetailResponse(
        Long reviewId,
        String username,
        RecipeSummary recipe,
        Integer rating,
        String comment,
        String imageUrl,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        boolean ownedByCurrentUser) {

    public record RecipeSummary(Long id, String title, String imageUrl, String domain) {
    }

    // Same imageUrl fallback rule as FeedItemResponse: the reviewer's own uploaded photo,
    // falling back to the recipe's scraped image when they haven't uploaded one.
    public static ReviewDetailResponse from(Review review, boolean ownedByCurrentUser) {
        ExternalRecipe recipe = review.getRecipe();
        String displayImageUrl = review.getImageUrl() != null ? review.getImageUrl() : recipe.getImageUrl();
        return new ReviewDetailResponse(
                review.getId(),
                review.getUser().getDisplayName(),
                new RecipeSummary(recipe.getId(), recipe.getTitle(), recipe.getImageUrl(), recipe.getDomain()),
                review.getRating(),
                review.getComment(),
                displayImageUrl,
                review.getCreatedAt(),
                review.getUpdatedAt(),
                ownedByCurrentUser);
    }
}
