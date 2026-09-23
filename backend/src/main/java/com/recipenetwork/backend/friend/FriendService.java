package com.recipenetwork.backend.friend;

import com.recipenetwork.backend.auth.User;
import com.recipenetwork.backend.auth.UserRepository;
import com.recipenetwork.backend.common.ApiException;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FriendService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    public FriendService(FriendshipRepository friendshipRepository, UserRepository userRepository) {
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
    }

    public record AddResult(FriendResponse friend, boolean newlyCreated) {
    }

    @Transactional
    public AddResult add(User user, String email) {
        if (email == null || email.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "E-postadress saknas.");
        }
        if (email.equalsIgnoreCase(user.getEmail())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CANNOT_ADD_SELF", "Du kan inte lägga till dig själv som kompis.");
        }

        User friend = userRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
                        "Hittade ingen användare med den e-postadressen."));

        return friendshipRepository.findByUser_IdAndFriend_Id(user.getId(), friend.getId())
                .map(existing -> new AddResult(FriendResponse.from(existing), false))
                .orElseGet(() -> createFriendship(user, friend));
    }

    private AddResult createFriendship(User user, User friend) {
        try {
            Friendship saved = friendshipRepository.save(new Friendship(user, friend, OffsetDateTime.now()));
            return new AddResult(FriendResponse.from(saved), true);
        } catch (DataIntegrityViolationException e) {
            // Lost a race with a concurrent add of the same friend.
            Friendship existing = friendshipRepository.findByUser_IdAndFriend_Id(user.getId(), friend.getId())
                    .orElseThrow(() -> e);
            return new AddResult(FriendResponse.from(existing), false);
        }
    }

    @Transactional
    public void remove(User user, Long friendUserId) {
        friendshipRepository.deleteByUser_IdAndFriend_Id(user.getId(), friendUserId);
    }

    @Transactional(readOnly = true)
    public List<FriendResponse> findFriends(User user) {
        return friendshipRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(FriendResponse::from)
                .toList();
    }
}
