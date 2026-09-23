package com.recipenetwork.backend.review;

import java.time.OffsetDateTime;

public record ReviewResponse(
        Long reviewId, Long recipeId, Integer rating, String comment, String imageUrl,
        OffsetDateTime createdAt, OffsetDateTime updatedAt) {

    // imageUrl is the reviewer's own uploaded photo, falling back to the recipe's
    // scraped image when they haven't uploaded one.
    public static ReviewResponse from(Review review) {
        String imageUrl = review.getImageUrl() != null ? review.getImageUrl() : review.getRecipe().getImageUrl();
        return new ReviewResponse(
                review.getId(), review.getRecipe().getId(), review.getRating(), review.getComment(), imageUrl,
                review.getCreatedAt(), review.getUpdatedAt());
    }
}
