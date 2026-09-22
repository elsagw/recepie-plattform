package com.recipenetwork.backend.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.recipenetwork.backend.auth.User;
import com.recipenetwork.backend.common.ApiException;
import com.recipenetwork.backend.recipe.ExternalRecipe;
import com.recipenetwork.backend.recipe.ExternalRecipeRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ExternalRecipeRepository externalRecipeRepository;

    @InjectMocks
    private ReviewService reviewService;

    private final User user = new User("google-sub-1", "elsa@example.com", "Elsa");
    private final ExternalRecipe recipe =
            new ExternalRecipe("https://example.com/", "Exempelrecept", null, "example.com", OffsetDateTime.now());

    @Test
    void createThrowsBadRequestWhenRecipeIdMissing() {
        var request = new CreateReviewRequest(null, 4, "bra");

        assertThatThrownBy(() -> reviewService.create(user, request))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6, -1})
    void createThrowsUnprocessableWhenRatingOutOfRange(int rating) {
        var request = new CreateReviewRequest(1L, rating, "test");

        assertThatThrownBy(() -> reviewService.create(user, request))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void createThrowsUnprocessableWhenCommentTooLong() {
        var request = new CreateReviewRequest(1L, 5, "x".repeat(2001));

        assertThatThrownBy(() -> reviewService.create(user, request))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("COMMENT_TOO_LONG");
    }

    @Test
    void createThrowsNotFoundWhenRecipeMissing() {
        when(externalRecipeRepository.findById(1L)).thenReturn(Optional.empty());
        var request = new CreateReviewRequest(1L, 5, "test");

        assertThatThrownBy(() -> reviewService.create(user, request))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createSavesAndReturnsReviewWhenValid() {
        when(externalRecipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewResponse response = reviewService.create(user, new CreateReviewRequest(1L, 5, "Jättegott"));

        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.comment()).isEqualTo("Jättegott");
    }

    @Test
    void updateThrowsNotFoundWhenReviewMissingOrNotOwned() {
        when(reviewRepository.findByIdAndUser_Id(99L, null)).thenReturn(Optional.empty());
        var request = new UpdateReviewRequest(3, "uppdaterad");

        assertThatThrownBy(() -> reviewService.update(user, 99L, request))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateChangesRatingAndCommentWhenOwned() {
        Review existing = new Review(user, recipe, 2, "meh", OffsetDateTime.now().minusDays(1));
        when(reviewRepository.findByIdAndUser_Id(1L, null)).thenReturn(Optional.of(existing));

        ReviewResponse response = reviewService.update(user, 1L, new UpdateReviewRequest(5, "Nu mycket bättre!"));

        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.comment()).isEqualTo("Nu mycket bättre!");
    }
}
