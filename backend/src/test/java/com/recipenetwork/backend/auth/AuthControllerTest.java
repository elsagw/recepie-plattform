package com.recipenetwork.backend.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

import com.recipenetwork.backend.common.ApiException;
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

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrentUserResolver currentUserResolver;

    @MockitoBean
    private AccountService accountService;

    private final User user = new User("google-sub-1", "elsa@example.com", "Elsa");

    {
        ReflectionTestUtils.setField(user, "id", 7L);
    }

    @Test
    void meReturns200WithCurrentUser() throws Exception {
        when(currentUserResolver.requireCurrentUser(any())).thenReturn(user);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("elsa@example.com"))
                .andExpect(jsonPath("$.displayName").value("Elsa"));
    }

    @Test
    void meReturns401WhenNotLoggedIn() throws Exception {
        when(currentUserResolver.requireCurrentUser(any()))
                .thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Du måste vara inloggad."));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void updateMeReturns200WithUpdatedProfile() throws Exception {
        when(currentUserResolver.requireCurrentUser(any())).thenReturn(user);
        when(accountService.updateProfile(eq(7L), any(UpdateAccountRequest.class)))
                .thenReturn(new CurrentUserResponse(7L, "ny@example.com", "Nytt Namn", null));

        mockMvc.perform(put("/api/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Nytt Namn\",\"email\":\"ny@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Nytt Namn"));
    }

    @Test
    void updateMeReturns422ForInvalidEmail() throws Exception {
        when(currentUserResolver.requireCurrentUser(any())).thenReturn(user);
        when(accountService.updateProfile(eq(7L), any(UpdateAccountRequest.class)))
                .thenThrow(new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_EMAIL", "Ogiltig e-postadress."));

        mockMvc.perform(put("/api/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Elsa\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_EMAIL"));
    }

    @Test
    void updateMeReturns409WhenEmailTaken() throws Exception {
        when(currentUserResolver.requireCurrentUser(any())).thenReturn(user);
        when(accountService.updateProfile(eq(7L), any(UpdateAccountRequest.class)))
                .thenThrow(new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "E-postadressen används redan."));

        mockMvc.perform(put("/api/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Elsa\",\"email\":\"taken@example.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test
    void updateAvatarReturns200WithNewAvatarUrl() throws Exception {
        when(currentUserResolver.requireCurrentUser(any())).thenReturn(user);
        when(accountService.updateAvatar(eq(7L), any()))
                .thenReturn(new CurrentUserResponse(7L, "elsa@example.com", "Elsa", "/uploads/avatar.jpg"));

        MockMultipartFile file =
                new MockMultipartFile("image", "avatar.jpg", MediaType.IMAGE_JPEG_VALUE, "fake-bytes".getBytes());

        mockMvc.perform(multipart("/api/auth/me/avatar").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("/uploads/avatar.jpg"));
    }
}
