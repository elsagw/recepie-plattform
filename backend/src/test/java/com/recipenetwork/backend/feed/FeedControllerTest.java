package com.recipenetwork.backend.feed;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.recipenetwork.backend.auth.CurrentUserResolver;
import com.recipenetwork.backend.auth.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FeedController.class)
@AutoConfigureMockMvc(addFilters = false)
class FeedControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FeedService feedService;

    @MockitoBean
    private CurrentUserResolver currentUserResolver;

    private final User user = new User("google-sub-1", "elsa@example.com", "Elsa");

    {
        ReflectionTestUtils.setField(user, "id", 7L);
    }

    @Test
    void feedReturnsPagedContentWithDefaultPaging() throws Exception {
        when(currentUserResolver.resolveCurrentUser(any())).thenReturn(Optional.of(user));
        when(feedService.getFeed(0, 20, 7L)).thenReturn(new FeedResponse(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void feedForwardsCustomPageAndSize() throws Exception {
        when(currentUserResolver.resolveCurrentUser(any())).thenReturn(Optional.of(user));
        when(feedService.getFeed(2, 5, 7L)).thenReturn(new FeedResponse(List.of(), 2, 5, 12));

        mockMvc.perform(get("/api/feed").param("page", "2").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(12));
    }

    @Test
    void feedPassesNullUserIdWhenAnonymous() throws Exception {
        when(currentUserResolver.resolveCurrentUser(any())).thenReturn(Optional.empty());
        when(feedService.getFeed(eq(0), eq(20), isNull())).thenReturn(new FeedResponse(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/feed")).andExpect(status().isOk());

        verify(feedService).getFeed(0, 20, null);
    }
}
