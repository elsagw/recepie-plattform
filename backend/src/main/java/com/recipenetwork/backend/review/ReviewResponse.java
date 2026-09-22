package com.recipenetwork.backend.review;

import java.time.OffsetDateTime;

public record ReviewResponse(
        Long reviewId, Long recipeId, Integer rating, String comment,
        OffsetDateTime createdAt, OffsetDateTime updatedAt) {

    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(), review.getRecipe().getId(), review.getRating(), review.getComment(),
                review.getCreatedAt(), review.getUpdatedAt());
    }
}
