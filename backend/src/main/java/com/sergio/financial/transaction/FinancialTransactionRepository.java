package com.sergio.financial.transaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, Long> {
    Optional<FinancialTransaction> findByIdAndUserId(Long id, Long userId);

    List<FinancialTransaction> findByUserIdAndDateGreaterThanEqualAndDateLessThanOrderByDateDescIdDesc(
            Long userId, LocalDate from, LocalDate until);

    boolean existsByUserIdAndDuplicateFingerprint(Long userId, String duplicateFingerprint);

    @Query("""
            select new com.sergio.financial.transaction.CategoryExpense(
                transaction.category.id, transaction.category.name, sum(transaction.amount))
            from FinancialTransaction transaction
            where transaction.user.id = :userId
              and transaction.date >= :from
              and transaction.date < :until
              and transaction.type = com.sergio.financial.transaction.TransactionType.EXPENSE
            group by transaction.category.id, transaction.category.name
            order by transaction.category.id
            """)
    List<CategoryExpense> sumExpensesByCategory(Long userId, LocalDate from, LocalDate until);
}
