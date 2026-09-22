package com.recipenetwork.backend.review;

import com.recipenetwork.backend.auth.User;
import com.recipenetwork.backend.common.ApiException;
import com.recipenetwork.backend.recipe.ExternalRecipe;
import com.recipenetwork.backend.recipe.ExternalRecipeRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ExternalRecipeRepository externalRecipeRepository;

    public ReviewService(ReviewRepository reviewRepository, ExternalRecipeRepository externalRecipeRepository) {
        this.reviewRepository = reviewRepository;
        this.externalRecipeRepository = externalRecipeRepository;
    }

    @Transactional
    public ReviewResponse create(User user, CreateReviewRequest request) {
        if (request.recipeId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "recipeId saknas.");
        }
        int rating = validateRating(request.rating());

        ExternalRecipe recipe = externalRecipeRepository.findById(request.recipeId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RECIPE_NOT_FOUND", "Receptet hittades inte."));

        Review review = new Review(user, recipe, rating, request.comment(), OffsetDateTime.now());
        return ReviewResponse.from(reviewRepository.save(review));
    }

    @Transactional
    public ReviewResponse update(User user, Long reviewId, UpdateReviewRequest request) {
        int rating = validateRating(request.rating());

        Review review = reviewRepository.findByIdAndUser_Id(reviewId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND", "Recensionen hittades inte."));

        review.update(rating, request.comment(), OffsetDateTime.now());
        return ReviewResponse.from(review);
    }

    @Transactional(readOnly = true)
    public List<MyReviewResponse> findMyReviews(User user, Long recipeId) {
        return reviewRepository.findByUser_IdAndRecipe_IdOrderByCreatedAtDesc(user.getId(), recipeId)
                .stream()
                .map(MyReviewResponse::from)
                .toList();
    }

    private int validateRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_RATING",
                    "Betyg måste vara ett heltal mellan 1 och 5.");
        }
        return rating;
    }
}
