package com.recipenetwork.backend.feed;

import com.recipenetwork.backend.recipe.ExternalRecipe;
import com.recipenetwork.backend.review.Review;
import java.time.OffsetDateTime;

public record FeedItemResponse(
        Long reviewId,
        String username,
        RecipeSummary recipe,
        Integer rating,
        String comment,
        OffsetDateTime createdAt,
        boolean savedByCurrentUser) {

    public record RecipeSummary(Long id, String title, String imageUrl, String domain) {
    }

    // savedByCurrentUser is always false until Etapp 4 (SavedRecipe) exists.
    public static FeedItemResponse from(Review review) {
        ExternalRecipe recipe = review.getRecipe();
        return new FeedItemResponse(
                review.getId(),
                review.getUser().getDisplayName(),
                new RecipeSummary(recipe.getId(), recipe.getTitle(), recipe.getImageUrl(), recipe.getDomain()),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                false);
    }
}
