package com.recipenetwork.backend.friend;

import java.time.OffsetDateTime;

public record FriendResponse(Long userId, String displayName, String avatarUrl, OffsetDateTime addedAt) {

    public static FriendResponse from(Friendship friendship) {
        return new FriendResponse(
                friendship.getFriend().getId(),
                friendship.getFriend().getDisplayName(),
                friendship.getFriend().getAvatarUrl(),
                friendship.getCreatedAt());
    }
}
