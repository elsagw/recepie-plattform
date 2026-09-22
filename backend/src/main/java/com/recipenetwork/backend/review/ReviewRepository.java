package com.recipenetwork.backend.review;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByIdAndUser_Id(Long id, Long userId);

    List<Review> findByUser_IdAndRecipe_IdOrderByCreatedAtDesc(Long userId, Long recipeId);

    @Query(
            value = "select r from Review r join fetch r.user join fetch r.recipe order by r.createdAt desc",
            countQuery = "select count(r) from Review r")
    Page<Review> findFeed(Pageable pageable);
}
