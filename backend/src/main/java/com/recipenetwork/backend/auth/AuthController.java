package com.recipenetwork.backend.auth;

import com.recipenetwork.backend.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public CurrentUserResponse me(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Du måste vara inloggad.");
        }

        String oauthSubject = principal.getAttribute("sub");
        User user = userRepository.findByOauthSubject(oauthSubject)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Du måste vara inloggad."));

        return new CurrentUserResponse(user.getId(), user.getEmail(), user.getDisplayName());
    }
}
