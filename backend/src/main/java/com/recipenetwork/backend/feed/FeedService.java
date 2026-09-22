package com.recipenetwork.backend.feed;

import com.recipenetwork.backend.saved.SavedRecipeRepository;
import com.recipenetwork.backend.review.Review;
import com.recipenetwork.backend.review.ReviewRepository;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ReviewRepository reviewRepository;
    private final SavedRecipeRepository savedRecipeRepository;

    public FeedService(ReviewRepository reviewRepository, SavedRecipeRepository savedRecipeRepository) {
        this.reviewRepository = reviewRepository;
        this.savedRecipeRepository = savedRecipeRepository;
    }

    @Transactional(readOnly = true)
    public FeedResponse getFeed(int page, int size, Long currentUserId) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<Review> reviewPage = reviewRepository.findFeed(pageable);

        Set<Long> savedRecipeIds = savedRecipeIdsForCurrentUser(currentUserId, reviewPage.getContent());

        List<FeedItemResponse> content = reviewPage.getContent().stream()
                .map(review -> FeedItemResponse.from(review, savedRecipeIds.contains(review.getRecipe().getId())))
                .toList();

        return new FeedResponse(content, safePage, safeSize, reviewPage.getTotalElements());
    }

    private Set<Long> savedRecipeIdsForCurrentUser(Long currentUserId, List<Review> reviews) {
        if (currentUserId == null || reviews.isEmpty()) {
            return Set.of();
        }
        List<Long> recipeIds = reviews.stream().map(review -> review.getRecipe().getId()).toList();
        return savedRecipeRepository.findSavedRecipeIds(currentUserId, recipeIds);
    }
}
