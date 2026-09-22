package com.recipenetwork.backend.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DomainExtractorTest {

    private final DomainExtractor extractor = new DomainExtractor();

    @Test
    void stripsWwwPrefix() {
        assertThat(extractor.extract("https://www.ica.se/recept/example")).isEqualTo("ica.se");
    }

    @Test
    void keepsDomainWithoutWww() {
        assertThat(extractor.extract("https://ica.se/recept/example")).isEqualTo("ica.se");
    }

    @Test
    void lowercasesDomain() {
        assertThat(extractor.extract("https://WWW.ICA.se/recept")).isEqualTo("ica.se");
    }

    @Test
    void returnsNullForInvalidUrl() {
        assertThat(extractor.extract("not a url")).isNull();
    }
}
