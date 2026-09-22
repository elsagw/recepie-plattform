package com.recipenetwork.backend.review;

import java.time.OffsetDateTime;

public record MyReviewResponse(Long reviewId, Integer rating, String comment, OffsetDateTime createdAt) {

    public static MyReviewResponse from(Review review) {
        return new MyReviewResponse(review.getId(), review.getRating(), review.getComment(), review.getCreatedAt());
    }
}
