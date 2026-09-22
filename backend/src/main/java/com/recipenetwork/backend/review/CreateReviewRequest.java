package com.recipenetwork.backend.review;

public record CreateReviewRequest(Long recipeId, Integer rating, String comment) {
}
