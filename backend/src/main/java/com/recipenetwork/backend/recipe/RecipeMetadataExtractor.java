package com.recipenetwork.backend.recipe;

import java.util.Optional;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Extracts title/image from a recipe page's HTML: OpenGraph tags first, then
 * Schema.org JSON-LD (type "Recipe"), then a plain {@code <title>} fallback for the title.
 * Either field can end up null — the frontend shows a placeholder image when it's missing.
 */
@Component
public class RecipeMetadataExtractor {

    private final ObjectMapper objectMapper;

    public RecipeMetadataExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public record Metadata(String title, String imageUrl) {
    }

    public Metadata extract(String html, String baseUri) {
        Document document = Jsoup.parse(html, baseUri);

        String title = openGraph(document, "og:title")
                .or(() -> jsonLdField(document, "name"))
                .or(() -> titleTag(document))
                .orElse(null);

        String imageUrl = openGraph(document, "og:image")
                .or(() -> jsonLdImage(document))
                .orElse(null);

        return new Metadata(title, imageUrl);
    }

    private Optional<String> openGraph(Document document, String property) {
        Element tag = document.selectFirst("meta[property=" + property + "]");
        if (tag == null) {
            return Optional.empty();
        }
        String content = tag.attr("content");
        return content.isBlank() ? Optional.empty() : Optional.of(content);
    }

    private Optional<String> titleTag(Document document) {
        String text = document.title();
        return text.isBlank() ? Optional.empty() : Optional.of(text);
    }

    private Optional<String> jsonLdField(Document document, String field) {
        return findRecipeJsonLd(document)
                .map(node -> node.path(field).asString(null))
                .filter(value -> value != null && !value.isBlank());
    }

    private Optional<String> jsonLdImage(Document document) {
        return findRecipeJsonLd(document).flatMap(this::extractImage);
    }

    private Optional<String> extractImage(JsonNode recipeNode) {
        JsonNode image = recipeNode.path("image");
        if (image.isTextual()) {
            return Optional.ofNullable(image.asString(null));
        }
        if (image.isArray() && !image.isEmpty()) {
            JsonNode first = image.get(0);
            if (first.isTextual()) {
                return Optional.ofNullable(first.asString(null));
            }
            if (first.isObject() && first.has("url")) {
                return Optional.ofNullable(first.path("url").asString(null));
            }
        }
        if (image.isObject() && image.has("url")) {
            return Optional.ofNullable(image.path("url").asString(null));
        }
        return Optional.empty();
    }

    private Optional<JsonNode> findRecipeJsonLd(Document document) {
        for (Element script : document.select("script[type=application/ld+json]")) {
            try {
                JsonNode node = objectMapper.readTree(script.data());
                Optional<JsonNode> recipe = findRecipeNode(node);
                if (recipe.isPresent()) {
                    return recipe;
                }
            } catch (RuntimeException e) {
                // Malformed JSON-LD block on the page; skip it and try the next one.
            }
        }
        return Optional.empty();
    }

    private Optional<JsonNode> findRecipeNode(JsonNode node) {
        if (node.isArray()) {
            for (JsonNode item : node) {
                Optional<JsonNode> found = findRecipeNode(item);
                if (found.isPresent()) {
                    return found;
                }
            }
            return Optional.empty();
        }
        if (node.isObject()) {
            JsonNode graph = node.path("@graph");
            if (graph.isArray()) {
                Optional<JsonNode> found = findRecipeNode(graph);
                if (found.isPresent()) {
                    return found;
                }
            }
            if (isRecipeType(node.path("@type"))) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    private boolean isRecipeType(JsonNode type) {
        if (type.isTextual()) {
            return "Recipe".equalsIgnoreCase(type.asString(null));
        }
        if (type.isArray()) {
            for (JsonNode item : type) {
                if (item.isTextual() && "Recipe".equalsIgnoreCase(item.asString(null))) {
                    return true;
                }
            }
        }
        return false;
    }
}
