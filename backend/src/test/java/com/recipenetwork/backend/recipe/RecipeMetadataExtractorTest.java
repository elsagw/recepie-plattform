package com.recipenetwork.backend.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class RecipeMetadataExtractorTest {

    private final RecipeMetadataExtractor extractor = new RecipeMetadataExtractor(new JsonMapper());

    @Test
    void extractsFromOpenGraph() {
        String html = """
                <html><head>
                <meta property="og:title" content="Krämig pasta"/>
                <meta property="og:image" content="https://cdn.example/pasta.jpg"/>
                </head><body></body></html>
                """;

        RecipeMetadataExtractor.Metadata metadata = extractor.extract(html, "https://example.com/recipe");

        assertThat(metadata.title()).isEqualTo("Krämig pasta");
        assertThat(metadata.imageUrl()).isEqualTo("https://cdn.example/pasta.jpg");
    }

    @Test
    void fallsBackToJsonLdRecipeWhenNoOpenGraph() {
        String html = """
                <html><head>
                <script type="application/ld+json">
                {"@context":"https://schema.org","@type":"Recipe","name":"Chokladbollar","image":"https://cdn.example/choklad.jpg"}
                </script>
                </head><body></body></html>
                """;

        RecipeMetadataExtractor.Metadata metadata = extractor.extract(html, "https://example.com/recipe");

        assertThat(metadata.title()).isEqualTo("Chokladbollar");
        assertThat(metadata.imageUrl()).isEqualTo("https://cdn.example/choklad.jpg");
    }

    @Test
    void handlesJsonLdImageAsArrayOfObjects() {
        String html = """
                <html><head>
                <script type="application/ld+json">
                {"@type":"Recipe","name":"Kanelbullar","image":[{"url":"https://cdn.example/bullar.jpg"}]}
                </script>
                </head><body></body></html>
                """;

        RecipeMetadataExtractor.Metadata metadata = extractor.extract(html, "https://example.com/recipe");

        assertThat(metadata.imageUrl()).isEqualTo("https://cdn.example/bullar.jpg");
    }

    @Test
    void handlesJsonLdInsideGraphArray() {
        String html = """
                <html><head>
                <script type="application/ld+json">
                {"@context":"https://schema.org","@graph":[{"@type":"WebPage"},{"@type":"Recipe","name":"Lasagne"}]}
                </script>
                </head><body></body></html>
                """;

        RecipeMetadataExtractor.Metadata metadata = extractor.extract(html, "https://example.com/recipe");

        assertThat(metadata.title()).isEqualTo("Lasagne");
    }

    @Test
    void fallsBackToTitleTagWhenNothingElseFound() {
        String html = "<html><head><title>Bara en titel</title></head><body></body></html>";

        RecipeMetadataExtractor.Metadata metadata = extractor.extract(html, "https://example.com/recipe");

        assertThat(metadata.title()).isEqualTo("Bara en titel");
        assertThat(metadata.imageUrl()).isNull();
    }

    @Test
    void toleratesMalformedJsonLd() {
        String html = """
                <html><head>
                <title>Fallback-titel</title>
                <script type="application/ld+json">{ not valid json </script>
                </head><body></body></html>
                """;

        RecipeMetadataExtractor.Metadata metadata = extractor.extract(html, "https://example.com/recipe");

        assertThat(metadata.title()).isEqualTo("Fallback-titel");
    }
}
