package com.sergio.financial.transaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, Long> {
    Optional<FinancialTransaction> findByIdAndUserId(Long id, Long userId);

    List<FinancialTransaction> findByUserIdAndDateGreaterThanEqualAndDateLessThanOrderByDateDescIdDesc(
            Long userId, LocalDate from, LocalDate until);

    boolean existsByUserIdAndDuplicateFingerprint(Long userId, String duplicateFingerprint);
}
