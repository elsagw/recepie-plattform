package com.recipenetwork.backend.recipe;

import com.recipenetwork.backend.common.ApiException;
import java.time.OffsetDateTime;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class RecipeScrapeService {

    private final ExternalRecipeRepository externalRecipeRepository;
    private final UrlNormalizer urlNormalizer;
    private final SafeHtmlFetcher safeHtmlFetcher;
    private final RecipeMetadataExtractor recipeMetadataExtractor;
    private final DomainExtractor domainExtractor;

    public RecipeScrapeService(
            ExternalRecipeRepository externalRecipeRepository,
            UrlNormalizer urlNormalizer,
            SafeHtmlFetcher safeHtmlFetcher,
            RecipeMetadataExtractor recipeMetadataExtractor,
            DomainExtractor domainExtractor) {
        this.externalRecipeRepository = externalRecipeRepository;
        this.urlNormalizer = urlNormalizer;
        this.safeHtmlFetcher = safeHtmlFetcher;
        this.recipeMetadataExtractor = recipeMetadataExtractor;
        this.domainExtractor = domainExtractor;
    }

    public ExternalRecipe scrape(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_URL", "URL saknas.");
        }

        String normalizedUrl = urlNormalizer.normalize(rawUrl);

        return externalRecipeRepository.findBySourceUrl(normalizedUrl)
                .orElseGet(() -> scrapeAndSave(normalizedUrl));
    }

    private ExternalRecipe scrapeAndSave(String normalizedUrl) {
        SafeHtmlFetcher.FetchResult result = safeHtmlFetcher.fetch(normalizedUrl);
        RecipeMetadataExtractor.Metadata metadata =
                recipeMetadataExtractor.extract(result.html(), result.finalUrl());
        String domain = domainExtractor.extract(result.finalUrl());

        ExternalRecipe recipe = new ExternalRecipe(
                normalizedUrl, metadata.title(), metadata.imageUrl(), domain, OffsetDateTime.now());

        try {
            return externalRecipeRepository.save(recipe);
        } catch (DataIntegrityViolationException e) {
            // Lost a race with a concurrent scrape of the same URL - return the winner's row.
            return externalRecipeRepository.findBySourceUrl(normalizedUrl).orElseThrow(() -> e);
        }
    }
}
