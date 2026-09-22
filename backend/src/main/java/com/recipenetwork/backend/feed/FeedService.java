package com.recipenetwork.backend.feed;

import com.recipenetwork.backend.review.Review;
import com.recipenetwork.backend.review.ReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ReviewRepository reviewRepository;

    public FeedService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @Transactional(readOnly = true)
    public FeedResponse getFeed(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<Review> reviewPage = reviewRepository.findFeed(pageable);

        return new FeedResponse(
                reviewPage.getContent().stream().map(FeedItemResponse::from).toList(),
                safePage,
                safeSize,
                reviewPage.getTotalElements());
    }
}
