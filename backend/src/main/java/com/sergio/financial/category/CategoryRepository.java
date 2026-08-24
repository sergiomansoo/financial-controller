package com.sergio.financial.category;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    long countBySystemCategoryTrue();

    @Query("select category from Category category where category.systemCategory = true or category.user.id = :userId order by category.id")
    List<Category> findAccessibleByUserId(Long userId);

    @Query("select category from Category category where category.id = :categoryId and (category.systemCategory = true or category.user.id = :userId)")
    Optional<Category> findAccessibleByIdAndUserId(Long categoryId, Long userId);

}
