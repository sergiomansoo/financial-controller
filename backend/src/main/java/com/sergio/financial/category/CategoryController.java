package com.sergio.financial.category;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final CategoryRepository categories;

    public CategoryController(CategoryRepository categories) {
        this.categories = categories;
    }

    @GetMapping
    public List<CategoryResponse> list(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        return categories.findAccessibleByUserId(userId).stream()
                .map(category -> new CategoryResponse(category.getId(), category.getName()))
                .toList();
    }
}
