package com.sergio.financial.rule;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRuleRepository extends JpaRepository<CategoryRule, Long> {
    Optional<CategoryRule> findByUserIdAndNormalizedDescription(Long userId, String normalizedDescription);
    Optional<CategoryRule> findByIdAndUserId(Long id, Long userId);
    List<CategoryRule> findByUserIdOrderByIdDesc(Long userId);
    boolean existsByCategoryId(Long categoryId);
}
