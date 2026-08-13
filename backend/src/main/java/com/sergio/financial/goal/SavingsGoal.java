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
@Table(name = "savings_goals")
public class SavingsGoal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne @JoinColumn(name = "user_id") private User user;
    private String name;
    private BigDecimal targetAmount;
    private LocalDate targetDate;
    protected SavingsGoal() { }
    public SavingsGoal(User user, String name, BigDecimal targetAmount, LocalDate targetDate) {
        this.user = user; this.name = name; this.targetAmount = targetAmount; this.targetDate = targetDate;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getTargetAmount() { return targetAmount; }
    public LocalDate getTargetDate() { return targetDate; }
}
