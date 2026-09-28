package com.recipenetwork.backend.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.recipenetwork.backend.common.ApiException;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class RecipeScrapeServiceTest {

    @Mock
    private ExternalRecipeRepository externalRecipeRepository;

    @Mock
    private UrlNormalizer urlNormalizer;

    @Mock
    private SafeHtmlFetcher safeHtmlFetcher;

    @Mock
    private RecipeMetadataExtractor recipeMetadataExtractor;

    @Mock
    private DomainExtractor domainExtractor;

    @InjectMocks
    private RecipeScrapeService recipeScrapeService;

    private static final String RAW_URL = "https://Example.com/recept?utm_source=x#top";
    private static final String NORMALIZED_URL = "https://example.com/recept";

    @Test
    void scrapeThrowsBadRequestWhenUrlBlank() {
        assertThatThrownBy(() -> recipeScrapeService.scrape("   "))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("INVALID_URL");
    }

    @Test
    void scrapeReturnsCachedRecipeOnCacheHitWithoutFetching() {
        when(urlNormalizer.normalize(RAW_URL)).thenReturn(NORMALIZED_URL);
        ExternalRecipe cached =
                new ExternalRecipe(NORMALIZED_URL, "Exempelrecept", null, "example.com", OffsetDateTime.now());
        when(externalRecipeRepository.findBySourceUrl(NORMALIZED_URL)).thenReturn(Optional.of(cached));

        ExternalRecipe result = recipeScrapeService.scrape(RAW_URL);

        assertThat(result).isSameAs(cached);
        verify(safeHtmlFetcher, never()).fetch(any());
    }

    @Test
    void scrapeFetchesAndSavesNewRecipeWhenNotCached() {
        when(urlNormalizer.normalize(RAW_URL)).thenReturn(NORMALIZED_URL);
        when(externalRecipeRepository.findBySourceUrl(NORMALIZED_URL)).thenReturn(Optional.empty());
        when(safeHtmlFetcher.fetch(NORMALIZED_URL))
                .thenReturn(new SafeHtmlFetcher.FetchResult("<html></html>", NORMALIZED_URL));
        when(recipeMetadataExtractor.extract("<html></html>", NORMALIZED_URL))
                .thenReturn(new RecipeMetadataExtractor.Metadata("Exempelrecept", "https://cdn.example/img.jpg"));
        when(domainExtractor.extract(NORMALIZED_URL)).thenReturn("example.com");
        when(externalRecipeRepository.save(any(ExternalRecipe.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ExternalRecipe result = recipeScrapeService.scrape(RAW_URL);

        assertThat(result.getSourceUrl()).isEqualTo(NORMALIZED_URL);
        assertThat(result.getTitle()).isEqualTo("Exempelrecept");
        assertThat(result.getDomain()).isEqualTo("example.com");
    }

    @Test
    void scrapeReturnsWinnersRowWhenLosingConcurrentSaveRace() {
        when(urlNormalizer.normalize(RAW_URL)).thenReturn(NORMALIZED_URL);
        when(externalRecipeRepository.findBySourceUrl(NORMALIZED_URL))
                .thenReturn(Optional.empty())
                // Second call - after losing the race on save() - returns the winner's row.
                .thenReturn(Optional.of(new ExternalRecipe(
                        NORMALIZED_URL, "Exempelrecept", null, "example.com", OffsetDateTime.now())));
        when(safeHtmlFetcher.fetch(NORMALIZED_URL))
                .thenReturn(new SafeHtmlFetcher.FetchResult("<html></html>", NORMALIZED_URL));
        when(recipeMetadataExtractor.extract(any(), any()))
                .thenReturn(new RecipeMetadataExtractor.Metadata("Exempelrecept", null));
        when(domainExtractor.extract(NORMALIZED_URL)).thenReturn("example.com");
        when(externalRecipeRepository.save(any(ExternalRecipe.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        ExternalRecipe result = recipeScrapeService.scrape(RAW_URL);

        assertThat(result.getTitle()).isEqualTo("Exempelrecept");
    }

    @Test
    void scrapePropagatesSsrfRejectionFromFetcher() {
        when(urlNormalizer.normalize(RAW_URL)).thenReturn(NORMALIZED_URL);
        when(externalRecipeRepository.findBySourceUrl(NORMALIZED_URL)).thenReturn(Optional.empty());
        when(safeHtmlFetcher.fetch(NORMALIZED_URL)).thenThrow(
                new ApiException(HttpStatus.BAD_REQUEST, "URL_NOT_ALLOWED", "URL:en pekar på ett internt nätverk."));

        assertThatThrownBy(() -> recipeScrapeService.scrape(RAW_URL))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("URL_NOT_ALLOWED");
    }

    @Test
    void scrapePropagatesTimeoutAsBadGateway() {
        when(urlNormalizer.normalize(RAW_URL)).thenReturn(NORMALIZED_URL);
        when(externalRecipeRepository.findBySourceUrl(NORMALIZED_URL)).thenReturn(Optional.empty());
        when(safeHtmlFetcher.fetch(NORMALIZED_URL))
                .thenThrow(new ApiException(HttpStatus.BAD_GATEWAY, "FETCH_FAILED", "Kunde inte hämta sidan."));

        assertThatThrownBy(() -> recipeScrapeService.scrape(RAW_URL))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_GATEWAY);
    }
}
