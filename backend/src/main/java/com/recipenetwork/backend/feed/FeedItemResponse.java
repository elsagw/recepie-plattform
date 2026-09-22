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
        OffsetDateTime updatedAt,
        boolean savedByCurrentUser) {

    public record RecipeSummary(Long id, String title, String imageUrl, String domain) {
    }

    // Feed order/position is always by createdAt - editing a review does not bump it to
    // the top. updatedAt is included so the frontend can show an "edited" indicator.
    public static FeedItemResponse from(Review review, boolean savedByCurrentUser) {
        ExternalRecipe recipe = review.getRecipe();
        return new FeedItemResponse(
                review.getId(),
                review.getUser().getDisplayName(),
                new RecipeSummary(recipe.getId(), recipe.getTitle(), recipe.getImageUrl(), recipe.getDomain()),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                savedByCurrentUser);
    }
}
