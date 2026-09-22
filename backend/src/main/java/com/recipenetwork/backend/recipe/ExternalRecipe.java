package com.recipenetwork.backend.recipe;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "external_recipes")
public class ExternalRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_url", nullable = false, unique = true, length = 2048)
    private String sourceUrl;

    @Column(length = 512)
    private String title;

    @Column(name = "image_url", length = 2048)
    private String imageUrl;

    @Column(nullable = false)
    private String domain;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ExternalRecipe() {
    }

    public ExternalRecipe(String sourceUrl, String title, String imageUrl, String domain, OffsetDateTime createdAt) {
        this.sourceUrl = sourceUrl;
        this.title = title;
        this.imageUrl = imageUrl;
        this.domain = domain;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getDomain() {
        return domain;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
