package com.sergio.financial.goal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SavingsGoalMonthRepository extends JpaRepository<SavingsGoalMonth, Long> {
    Optional<SavingsGoalMonth> findByUserIdAndGoalIdAndReferenceMonth(Long userId, Long goalId, LocalDate referenceMonth);
    List<SavingsGoalMonth> findByUserIdAndGoalId(Long userId, Long goalId);
    long countByUserIdAndGoalId(Long userId, Long goalId);
    void deleteByUserIdAndGoalId(Long userId, Long goalId);
}
