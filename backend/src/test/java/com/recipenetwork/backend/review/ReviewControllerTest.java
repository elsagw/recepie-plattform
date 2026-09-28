package com.recipenetwork.backend.review;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recipenetwork.backend.auth.CurrentUserResolver;
import com.recipenetwork.backend.auth.User;
import com.recipenetwork.backend.common.ApiException;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private CurrentUserResolver currentUserResolver;

    private final User user = new User("google-sub-1", "elsa@example.com", "Elsa");

    {
        ReflectionTestUtils.setField(user, "id", 7L);
    }

    private static final ReviewResponse REVIEW_RESPONSE = new ReviewResponse(
            1L, 12L, 5, "Jättegott", null, OffsetDateTime.now(), OffsetDateTime.now());

    @Test
    void createReturns201WhenLoggedIn() throws Exception {
        when(currentUserResolver.requireCurrentUser(any())).thenReturn(user);
        when(reviewService.create(eq(user), any(CreateReviewRequest.class))).thenReturn(REVIEW_RESPONSE);

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipeId\":12,\"rating\":5,\"comment\":\"Jättegott\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Jättegott"));
    }

    @Test
    void createReturns401WhenNotLoggedIn() throws Exception {
        when(currentUserResolver.requireCurrentUser(any()))
                .thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Du måste vara inloggad."));

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipeId\":12,\"rating\":5,\"comment\":\"bra\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void createReturns422ForInvalidRating() throws Exception {
        when(currentUserResolver.requireCurrentUser(any())).thenReturn(user);
        when(reviewService.create(eq(user), any(CreateReviewRequest.class)))
                .thenThrow(new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_RATING",
                        "Betyg måste vara ett heltal mellan 1 och 5."));

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipeId\":12,\"rating\":9,\"comment\":\"bra\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_RATING"));
    }

    @Test
    void updateReturns200WithUpdatedReview() throws Exception {
        when(currentUserResolver.requireCurrentUser(any())).thenReturn(user);
        ReviewResponse updated = new ReviewResponse(
                1L, 12L, 4, "Uppdaterad", null, OffsetDateTime.now(), OffsetDateTime.now());
        when(reviewService.update(eq(user), eq(1L), any(UpdateReviewRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/reviews/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4,\"comment\":\"Uppdaterad\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4));
    }

    @Test
    void updateReturns404WhenReviewNotOwned() throws Exception {
        when(currentUserResolver.requireCurrentUser(any())).thenReturn(user);
        when(reviewService.update(eq(user), eq(99L), any(UpdateReviewRequest.class)))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND", "Recensionen hittades inte."));

        mockMvc.perform(put("/api/reviews/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4,\"comment\":\"x\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REVIEW_NOT_FOUND"));
    }

    @Test
    void uploadImageReturns200WithReview() throws Exception {
        when(currentUserResolver.requireCurrentUser(any())).thenReturn(user);
        when(reviewService.setImage(eq(user), eq(1L), any())).thenReturn(REVIEW_RESPONSE);

        MockMultipartFile file =
                new MockMultipartFile("image", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-bytes".getBytes());

        mockMvc.perform(multipart("/api/reviews/1/image").file(file))
                .andExpect(status().isOk());
    }

    @Test
    void mineReturnsPastReviewsForRecipe() throws Exception {
        when(currentUserResolver.requireCurrentUser(any())).thenReturn(user);
        List<MyReviewResponse> reviews =
                List.of(new MyReviewResponse(40L, 4, "Bra men lite salt.", OffsetDateTime.now(), OffsetDateTime.now()));
        when(reviewService.findMyReviews(user, 12L)).thenReturn(reviews);

        mockMvc.perform(get("/api/recipes/12/reviews/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reviewId").value(40))
                .andExpect(jsonPath("$[0].rating").value(4));
    }

    @Test
    void mineReturnsEmptyListWhenNoPastReviews() throws Exception {
        when(currentUserResolver.requireCurrentUser(any())).thenReturn(user);
        when(reviewService.findMyReviews(user, 12L)).thenReturn(List.of());

        mockMvc.perform(get("/api/recipes/12/reviews/mine"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getReturnsReviewDetail() throws Exception {
        when(currentUserResolver.requireCurrentUser(any())).thenReturn(user);
        var detail = new ReviewDetailResponse(
                1L, "Elsa", null, new ReviewDetailResponse.RecipeSummary(12L, "Exempelrecept", null, "example.com"),
                5, "Jättegott", null, OffsetDateTime.now(), OffsetDateTime.now(), true);
        when(reviewService.getDetail(eq(1L), eq(7L))).thenReturn(detail);

        mockMvc.perform(get("/api/reviews/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownedByCurrentUser").value(true));
    }

    @Test
    void getReturns404WhenMissing() throws Exception {
        when(currentUserResolver.requireCurrentUser(any())).thenReturn(user);
        when(reviewService.getDetail(eq(99L), eq(7L)))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND", "Recensionen hittades inte."));

        mockMvc.perform(get("/api/reviews/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns204() throws Exception {
        when(currentUserResolver.requireCurrentUser(any())).thenReturn(user);

        mockMvc.perform(delete("/api/reviews/1")).andExpect(status().isNoContent());

        verify(reviewService).delete(user, 1L);
    }
}
