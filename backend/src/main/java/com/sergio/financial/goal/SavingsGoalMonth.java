package com.sergio.financial.goal;

import com.sergio.financial.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "savings_goal_months")
public class SavingsGoalMonth {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne @JoinColumn(name = "user_id") private User user;
    @ManyToOne @JoinColumn(name = "goal_id") private SavingsGoal goal;
    private LocalDate referenceMonth;
    private BigDecimal plannedAmount;
    private BigDecimal savedAmount;
    protected SavingsGoalMonth() { }
    public SavingsGoalMonth(User user, SavingsGoal goal, LocalDate referenceMonth, BigDecimal plannedAmount, BigDecimal savedAmount) {
        this.user = user; this.goal = goal; this.referenceMonth = referenceMonth;
        this.plannedAmount = plannedAmount; this.savedAmount = savedAmount;
    }
    public void update(BigDecimal plannedAmount, BigDecimal savedAmount) { this.plannedAmount = plannedAmount; this.savedAmount = savedAmount; }
    public BigDecimal getPlannedAmount() { return plannedAmount; }
    public BigDecimal getSavedAmount() { return savedAmount; }
}
