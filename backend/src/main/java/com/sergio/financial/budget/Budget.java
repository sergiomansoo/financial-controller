package com.sergio.financial.budget;

import com.sergio.financial.category.Category;
import com.sergio.financial.user.User;
import jakarta.persistence.Column;
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
@Table(name = "budgets")
public class Budget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "budget_month")
    private LocalDate month;

    @Column(name = "amount_limit")
    private BigDecimal limit;

    protected Budget() {
    }

    public Budget(User user, Category category, LocalDate month, BigDecimal limit) {
        this.user = user;
        this.category = category;
        this.month = month;
        this.limit = limit;
    }

    public void setLimit(BigDecimal limit) {
        this.limit = limit;
    }

    public Category getCategory() {
        return category;
    }

    public BigDecimal getLimit() {
        return limit;
    }
}
