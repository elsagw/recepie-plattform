package com.recipenetwork.backend.auth;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CurrentUserResolver currentUserResolver;
    private final AccountService accountService;

    public AuthController(CurrentUserResolver currentUserResolver, AccountService accountService) {
        this.currentUserResolver = currentUserResolver;
        this.accountService = accountService;
    }

    @GetMapping("/me")
    public CurrentUserResponse me(@AuthenticationPrincipal OAuth2User principal) {
        User user = currentUserResolver.requireCurrentUser(principal);
        return new CurrentUserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getAvatarUrl());
    }

    @PutMapping("/me")
    public CurrentUserResponse updateMe(
            @AuthenticationPrincipal OAuth2User principal, @RequestBody UpdateAccountRequest request) {
        User user = currentUserResolver.requireCurrentUser(principal);
        return accountService.updateProfile(user.getId(), request);
    }

    @PostMapping("/me/avatar")
    public CurrentUserResponse updateAvatar(
            @AuthenticationPrincipal OAuth2User principal, @RequestParam("image") MultipartFile image) {
        User user = currentUserResolver.requireCurrentUser(principal);
        return accountService.updateAvatar(user.getId(), image);
    }
}
