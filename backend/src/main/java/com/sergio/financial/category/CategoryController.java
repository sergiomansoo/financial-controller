package com.sergio.financial.category;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final CategoryRepository categories;
    private final CategoryService service;

    public CategoryController(CategoryRepository categories, CategoryService service) {
        this.categories = categories;
        this.service = service;
    }

    @GetMapping
    public List<CategoryResponse> list(Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        return categories.findAccessibleByUserId(userId).stream()
                .map(category -> new CategoryResponse(category.getId(), category.getName()))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request, Authentication authentication) {
        return service.create(Long.valueOf(authentication.getName()), request);
    }
}
