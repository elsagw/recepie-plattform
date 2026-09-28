package com.recipenetwork.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recipenetwork.backend.auth.User;
import com.recipenetwork.backend.auth.UserRepository;
import com.recipenetwork.backend.feed.FeedResponse;
import com.recipenetwork.backend.feed.FeedService;
import com.recipenetwork.backend.recipe.ExternalRecipe;
import com.recipenetwork.backend.recipe.ExternalRecipeRepository;
import com.recipenetwork.backend.review.Review;
import com.recipenetwork.backend.review.ReviewRepository;
import com.recipenetwork.backend.saved.SavedRecipe;
import com.recipenetwork.backend.saved.SavedRecipeRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Integration coverage against a real, disposable PostgreSQL instance (Testcontainers) -
 * distinct from the docker-compose Postgres the other Spring context tests use, so this
 * suite is self-contained and doesn't depend on `docker compose up` having been run first.
 * Covers constraints, multi-row relationships and feed pagination the way they actually
 * behave against the database, not against a mocked repository.
 */
@SpringBootTest
@Testcontainers
@Transactional
class RepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExternalRecipeRepository externalRecipeRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private SavedRecipeRepository savedRecipeRepository;

    @Autowired
    private FeedService feedService;

    @Test
    void externalRecipeSourceUrlIsUnique() {
        externalRecipeRepository.save(
                new ExternalRecipe("https://example.com/a", "A", null, "example.com", OffsetDateTime.now()));
        externalRecipeRepository.flush();

        assertThatThrownBy(() -> {
            externalRecipeRepository.save(
                    new ExternalRecipe("https://example.com/a", "A igen", null, "example.com", OffsetDateTime.now()));
            externalRecipeRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void savedRecipeUserRecipePairIsUnique() {
        User user = userRepository.save(new User("sub-unique-save", "unique-save@example.com", "Testaren"));
        ExternalRecipe recipe = externalRecipeRepository.save(
                new ExternalRecipe("https://example.com/save-test", "Test", null, "example.com", OffsetDateTime.now()));

        savedRecipeRepository.save(new SavedRecipe(user, recipe, OffsetDateTime.now()));
        savedRecipeRepository.flush();

        assertThatThrownBy(() -> {
            savedRecipeRepository.save(new SavedRecipe(user, recipe, OffsetDateTime.now()));
            savedRecipeRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void multipleReviewsAreAllowedForSameUserAndRecipe() {
        User user = userRepository.save(new User("sub-multi-review", "multi-review@example.com", "Testaren"));
        ExternalRecipe recipe = externalRecipeRepository.save(
                new ExternalRecipe("https://example.com/multi-review", "Test", null, "example.com", OffsetDateTime.now()));

        reviewRepository.save(new Review(user, recipe, 3, "Okej första gången", OffsetDateTime.now()));
        reviewRepository.save(new Review(user, recipe, 5, "Mycket bättre andra gången!", OffsetDateTime.now()));
        reviewRepository.flush();

        List<Review> reviews =
                reviewRepository.findByUser_IdAndRecipe_IdOrderByCreatedAtDesc(user.getId(), recipe.getId());

        assertThat(reviews).hasSize(2);
    }

    @Test
    void feedPaginatesAndOrdersNewestFirst() {
        User user = userRepository.save(new User("sub-feed-page", "feed-page@example.com", "Testaren"));
        ExternalRecipe recipe = externalRecipeRepository.save(
                new ExternalRecipe("https://example.com/feed-page", "Test", null, "example.com", OffsetDateTime.now()));

        OffsetDateTime now = OffsetDateTime.now();
        reviewRepository.save(new Review(user, recipe, 1, "first", now.minusMinutes(3)));
        reviewRepository.save(new Review(user, recipe, 2, "second", now.minusMinutes(2)));
        reviewRepository.save(new Review(user, recipe, 3, "third", now.minusMinutes(1)));
        reviewRepository.flush();

        FeedResponse firstPage = feedService.getFeed(0, 2, null);
        assertThat(firstPage.content()).hasSize(2);
        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.content().get(0).comment()).isEqualTo("third");
        assertThat(firstPage.content().get(1).comment()).isEqualTo("second");

        FeedResponse secondPage = feedService.getFeed(1, 2, null);
        assertThat(secondPage.content()).hasSize(1);
        assertThat(secondPage.content().get(0).comment()).isEqualTo("first");
    }
}
