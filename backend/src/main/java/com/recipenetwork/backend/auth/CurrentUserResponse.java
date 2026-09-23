package com.recipenetwork.backend.auth;

public record CurrentUserResponse(Long id, String email, String displayName, String avatarUrl) {
}
