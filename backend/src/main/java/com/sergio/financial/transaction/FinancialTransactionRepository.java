package com.sergio.financial.transaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, Long> {
    Optional<FinancialTransaction> findByIdAndUserId(Long id, Long userId);

    List<FinancialTransaction> findByUserIdAndDateGreaterThanEqualAndDateLessThanOrderByDateDescIdDesc(
            Long userId, LocalDate from, LocalDate until);

    boolean existsByUserIdAndDuplicateFingerprint(Long userId, String duplicateFingerprint);

    @Query("""
            select transaction from FinancialTransaction transaction
            where transaction.user.id = :userId
              and transaction.date >= :monthFrom and transaction.date < :monthUntil
              and (:type is null or transaction.type = :type)
              and (:categoryId is null or transaction.category.id = :categoryId)
              and (:fromDate is null or transaction.date >= :fromDate)
              and (:toDate is null or transaction.date <= :toDate)
            order by transaction.date desc, transaction.id desc
            """)
    Page<FinancialTransaction> search(@Param("userId") Long userId, @Param("monthFrom") LocalDate monthFrom,
                                      @Param("monthUntil") LocalDate monthUntil, @Param("type") TransactionType type,
                                      @Param("categoryId") Long categoryId, @Param("fromDate") LocalDate fromDate,
                                      @Param("toDate") LocalDate toDate, Pageable pageable);

    @Query("""
            select new com.sergio.financial.transaction.CategoryExpense(
                transaction.category.id, transaction.category.name, sum(transaction.amount))
            from FinancialTransaction transaction
            where transaction.user.id = :userId
              and transaction.date >= :from and transaction.date < :until
              and (:type is null or transaction.type = :type)
            group by transaction.category.id, transaction.category.name
            order by sum(transaction.amount) desc, transaction.category.name
            """)
    List<CategoryExpense> sumByCategory(Long userId, LocalDate from, LocalDate until, TransactionType type);

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
