package com.recipenetwork.backend.saved;

import com.recipenetwork.backend.auth.User;
import com.recipenetwork.backend.recipe.ExternalRecipe;
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
 * Saving is binary, unlike reviews - a unique (user_id, recipe_id) constraint enforces
 * "saved at most once" at the database level.
 */
@Entity
@Table(name = "saved_recipes")
public class SavedRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private ExternalRecipe recipe;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected SavedRecipe() {
    }

    public SavedRecipe(User user, ExternalRecipe recipe, OffsetDateTime createdAt) {
        this.user = user;
        this.recipe = recipe;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public ExternalRecipe getRecipe() {
        return recipe;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
