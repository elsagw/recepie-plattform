package com.recipenetwork.backend.auth;

import com.recipenetwork.backend.common.ApiException;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

/**
 * Resolves the local {@link User} row for the current OAuth2 principal. Shared by every
 * controller that needs "the logged-in user" (reviews, saved recipes, ...), so the
 * sub-claim lookup and the 401-on-missing-principal behavior stay in one place.
 */
@Component
public class CurrentUserResolver {

    private final UserRepository userRepository;

    public CurrentUserResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireCurrentUser(OAuth2User principal) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Du måste vara inloggad.");
        }

        String oauthSubject = principal.getAttribute("sub");
        return userRepository.findByOauthSubject(oauthSubject)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Du måste vara inloggad."));
    }

    /** Non-throwing variant for endpoints that behave differently for logged-in vs. anonymous callers (the feed). */
    public Optional<User> resolveCurrentUser(OAuth2User principal) {
        if (principal == null) {
            return Optional.empty();
        }
        String oauthSubject = principal.getAttribute("sub");
        return userRepository.findByOauthSubject(oauthSubject);
    }
}
