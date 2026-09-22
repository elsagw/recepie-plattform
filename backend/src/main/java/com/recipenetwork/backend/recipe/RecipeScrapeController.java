package com.recipenetwork.backend.recipe;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recipes")
public class RecipeScrapeController {

    private final RecipeScrapeService recipeScrapeService;

    public RecipeScrapeController(RecipeScrapeService recipeScrapeService) {
        this.recipeScrapeService = recipeScrapeService;
    }

    @PostMapping("/scrape")
    public ExternalRecipeResponse scrape(@RequestParam String url) {
        ExternalRecipe recipe = recipeScrapeService.scrape(url);
        return ExternalRecipeResponse.from(recipe);
    }
}
