package com.recipenetwork.backend.recipe;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalRecipeRepository extends JpaRepository<ExternalRecipe, Long> {

    Optional<ExternalRecipe> findBySourceUrl(String sourceUrl);
}
