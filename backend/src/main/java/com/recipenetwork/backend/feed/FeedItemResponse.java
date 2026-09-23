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
        String imageUrl,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        boolean savedByCurrentUser) {

    public record RecipeSummary(Long id, String title, String imageUrl, String domain) {
    }

    // Feed order/position is always by createdAt - editing a review does not bump it to
    // the top. updatedAt is included so the frontend can show an "edited" indicator.
    // imageUrl is the reviewer's own uploaded photo, falling back to the recipe's scraped
    // image when they haven't uploaded one - distinct from recipe.imageUrl, which is
    // always the recipe's own scraped image regardless of what this review displays.
    public static FeedItemResponse from(Review review, boolean savedByCurrentUser) {
        ExternalRecipe recipe = review.getRecipe();
        String displayImageUrl = review.getImageUrl() != null ? review.getImageUrl() : recipe.getImageUrl();
        return new FeedItemResponse(
                review.getId(),
                review.getUser().getDisplayName(),
                new RecipeSummary(recipe.getId(), recipe.getTitle(), recipe.getImageUrl(), recipe.getDomain()),
                review.getRating(),
                review.getComment(),
                displayImageUrl,
                review.getCreatedAt(),
                review.getUpdatedAt(),
                savedByCurrentUser);
    }
}
