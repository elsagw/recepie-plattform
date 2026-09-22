package com.recipenetwork.backend.feed;

import com.recipenetwork.backend.auth.CurrentUserResolver;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeedController {

    private final FeedService feedService;
    private final CurrentUserResolver currentUserResolver;

    public FeedController(FeedService feedService, CurrentUserResolver currentUserResolver) {
        this.feedService = feedService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping("/api/feed")
    public FeedResponse feed(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long currentUserId = currentUserResolver.resolveCurrentUser(principal).map(user -> user.getId()).orElse(null);
        return feedService.getFeed(page, size, currentUserId);
    }
}
