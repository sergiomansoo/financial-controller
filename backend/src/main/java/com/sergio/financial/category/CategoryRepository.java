package com.sergio.financial.category;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Query("select category from Category category where category.systemCategory = true or category.user.id = :userId order by category.id")
    List<Category> findAccessibleByUserId(Long userId);
}
