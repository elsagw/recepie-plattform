package com.recipenetwork.backend.saved;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SavedRecipeRepository extends JpaRepository<SavedRecipe, Long> {

    Optional<SavedRecipe> findByUser_IdAndRecipe_Id(Long userId, Long recipeId);

    @Query("select sr from SavedRecipe sr join fetch sr.recipe where sr.user.id = :userId order by sr.createdAt desc")
    List<SavedRecipe> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    void deleteByUser_IdAndRecipe_Id(Long userId, Long recipeId);

    @Query("select sr.recipe.id from SavedRecipe sr where sr.user.id = :userId and sr.recipe.id in :recipeIds")
    Set<Long> findSavedRecipeIds(@Param("userId") Long userId, @Param("recipeIds") Collection<Long> recipeIds);
}
