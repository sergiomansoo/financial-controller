package com.sergio.financial.budget;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    Optional<Budget> findByUserIdAndCategoryIdAndMonth(Long userId, Long categoryId, LocalDate month);

    @Query("""
            select budget from Budget budget
            join fetch budget.category
            where budget.user.id = :userId and budget.month = :month
            order by budget.category.id
            """)
    List<Budget> findByUserIdAndMonth(Long userId, LocalDate month);
}
