package com.recipenetwork.backend.auth;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CurrentUserResolver currentUserResolver;

    public AuthController(CurrentUserResolver currentUserResolver) {
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping("/me")
    public CurrentUserResponse me(@AuthenticationPrincipal OAuth2User principal) {
        User user = currentUserResolver.requireCurrentUser(principal);
        return new CurrentUserResponse(user.getId(), user.getEmail(), user.getDisplayName());
    }
}
