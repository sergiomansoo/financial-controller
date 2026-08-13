package com.sergio.financial.category;

import com.sergio.financial.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {
    private final CategoryRepository categories;
    private final UserRepository users;

    public CategoryService(CategoryRepository categories, UserRepository users) {
        this.categories = categories;
        this.users = users;
    }

    @Transactional
    public CategoryResponse create(Long userId, CategoryRequest request) {
        Category category = categories.save(new Category(users.getReferenceById(userId), request.name().trim()));
        return new CategoryResponse(category.getId(), category.getName());
    }
}
