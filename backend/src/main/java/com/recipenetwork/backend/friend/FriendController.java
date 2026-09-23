package com.recipenetwork.backend.friend;

import com.recipenetwork.backend.auth.CurrentUserResolver;
import com.recipenetwork.backend.auth.User;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;
    private final CurrentUserResolver currentUserResolver;

    public FriendController(FriendService friendService, CurrentUserResolver currentUserResolver) {
        this.friendService = friendService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping
    public ResponseEntity<FriendResponse> add(
            @AuthenticationPrincipal OAuth2User principal, @RequestBody AddFriendRequest request) {
        User user = currentUserResolver.requireCurrentUser(principal);
        FriendService.AddResult result = friendService.add(user, request.email());
        HttpStatus status = result.newlyCreated() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.friend());
    }

    @GetMapping
    public List<FriendResponse> list(@AuthenticationPrincipal OAuth2User principal) {
        User user = currentUserResolver.requireCurrentUser(principal);
        return friendService.findFriends(user);
    }

    @DeleteMapping("/{friendUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal OAuth2User principal, @PathVariable Long friendUserId) {
        User user = currentUserResolver.requireCurrentUser(principal);
        friendService.remove(user, friendUserId);
    }
}
