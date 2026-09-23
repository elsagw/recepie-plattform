package com.recipenetwork.backend.auth;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Auto-provisions a {@link User} on first Google login, keyed by the OAuth2 "sub" claim.
 *
 * Google's registration requests the "openid" scope, which makes this an OIDC login, not a
 * plain OAuth2 one. Spring Security routes OIDC logins through a separate hook
 * (userInfoEndpoint().oidcUserService(...), backed by OidcUserService) rather than the plain
 * OAuth2UserService one — extending DefaultOAuth2UserService here would silently never run.
 */
@Service
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;

    public CustomOidcUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String oauthSubject = oidcUser.getAttribute("sub");
        String email = oidcUser.getAttribute("email");
        String displayName = oidcUser.getAttribute("name");
        String picture = oidcUser.getAttribute("picture");

        // Only seeds avatarUrl from Google's profile picture on first provisioning - once a
        // user has an avatar (whether that initial Google picture or a later custom upload),
        // later logins never silently overwrite it.
        userRepository.findByOauthSubject(oauthSubject).orElseGet(() -> {
            User created = new User(oauthSubject, email, displayName);
            created.setAvatarUrl(picture);
            return userRepository.save(created);
        });

        return oidcUser;
    }
}
