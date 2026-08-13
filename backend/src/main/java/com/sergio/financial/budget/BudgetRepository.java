package com.sergio.financial.budget;
import java.time.LocalDate; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface BudgetRepository extends JpaRepository<Budget,Long>{ Optional<Budget> findByUserIdAndCategoryIdAndMonth(Long userId,Long categoryId,LocalDate month); List<Budget> findByUserIdAndMonth(Long userId,LocalDate month); }
