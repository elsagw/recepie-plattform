package com.recipenetwork.backend.friend;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    Optional<Friendship> findByUser_IdAndFriend_Id(Long userId, Long friendId);

    @Query("select f from Friendship f join fetch f.friend where f.user.id = :userId order by f.createdAt desc")
    List<Friendship> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    void deleteByUser_IdAndFriend_Id(Long userId, Long friendId);
}
