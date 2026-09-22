package com.recipenetwork.backend.review;

import com.recipenetwork.backend.auth.CurrentUserResolver;
import com.recipenetwork.backend.auth.User;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReviewController {

    private final ReviewService reviewService;
    private final CurrentUserResolver currentUserResolver;

    public ReviewController(ReviewService reviewService, CurrentUserResolver currentUserResolver) {
        this.reviewService = reviewService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/api/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@AuthenticationPrincipal OAuth2User principal, @RequestBody CreateReviewRequest request) {
        User user = currentUserResolver.requireCurrentUser(principal);
        return reviewService.create(user, request);
    }

    @PutMapping("/api/reviews/{reviewId}")
    public ReviewResponse update(
            @AuthenticationPrincipal OAuth2User principal,
            @PathVariable Long reviewId,
            @RequestBody UpdateReviewRequest request) {
        User user = currentUserResolver.requireCurrentUser(principal);
        return reviewService.update(user, reviewId, request);
    }

    @GetMapping("/api/recipes/{recipeId}/reviews/mine")
    public List<MyReviewResponse> mine(@AuthenticationPrincipal OAuth2User principal, @PathVariable Long recipeId) {
        User user = currentUserResolver.requireCurrentUser(principal);
        return reviewService.findMyReviews(user, recipeId);
    }
}
