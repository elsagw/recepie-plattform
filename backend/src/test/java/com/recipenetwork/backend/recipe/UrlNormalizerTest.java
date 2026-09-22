package com.recipenetwork.backend.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UrlNormalizerTest {

    private final UrlNormalizer normalizer = new UrlNormalizer();

    @Test
    void trimsWhitespace() {
        assertThat(normalizer.normalize("  https://example.com/recipe  "))
                .isEqualTo("https://example.com/recipe");
    }

    @Test
    void dropsFragment() {
        assertThat(normalizer.normalize("https://example.com/recipe#comments"))
                .isEqualTo("https://example.com/recipe");
    }

    @Test
    void stripsUtmTrackingParams() {
        assertThat(normalizer.normalize(
                "https://example.com/recipe?utm_source=fb&utm_medium=social&id=12"))
                .isEqualTo("https://example.com/recipe?id=12");
    }

    @Test
    void stripsFbclidAndGclid() {
        assertThat(normalizer.normalize("https://example.com/recipe?fbclid=abc&gclid=xyz"))
                .isEqualTo("https://example.com/recipe");
    }

    @Test
    void keepsNonTrackingQueryParams() {
        assertThat(normalizer.normalize("https://example.com/recipe?id=12&lang=sv"))
                .isEqualTo("https://example.com/recipe?id=12&lang=sv");
    }

    @Test
    void handlesUrlWithNoQueryOrFragment() {
        assertThat(normalizer.normalize("https://example.com/recipe"))
                .isEqualTo("https://example.com/recipe");
    }
}
