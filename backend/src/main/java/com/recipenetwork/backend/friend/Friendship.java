package com.recipenetwork.backend.friend;

import com.recipenetwork.backend.auth.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * A one-directional "added as friend" relationship - no request/accept flow. Adding
 * someone doesn't require them to confirm, and doesn't yet affect what either of you
 * sees (the feed stays global) - this is deliberately just the relationship data as a
 * foundation, per an explicit product decision to keep it minimal for now.
 */
@Entity
@Table(name = "friendships")
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "friend_user_id", nullable = false)
    private User friend;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected Friendship() {
    }

    public Friendship(User user, User friend, OffsetDateTime createdAt) {
        this.user = user;
        this.friend = friend;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public User getFriend() {
        return friend;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
