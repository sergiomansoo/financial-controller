package com.sergio.financial.transaction;

import com.sergio.financial.category.Category;
import com.sergio.financial.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transactions")
public class FinancialTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate date;

    private String history;

    @Column
    private String description;

    @Column(name = "normalized_description", nullable = false)
    private String normalizedDescription;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(name = "duplicate_fingerprint")
    private String duplicateFingerprint;

    @Column(name = "needs_review", nullable = false)
    private boolean needsReview;

    protected FinancialTransaction() {
    }

    public FinancialTransaction(User user, Category category, LocalDate date, String history, String description,
                                String normalizedDescription, BigDecimal amount, TransactionType type,
                                String duplicateFingerprint, boolean needsReview) {
        this.user = user;
        this.category = category;
        this.date = date;
        this.history = history;
        this.description = description;
        this.normalizedDescription = normalizedDescription;
        this.amount = amount;
        this.type = type;
        this.duplicateFingerprint = duplicateFingerprint;
        this.needsReview = needsReview;
    }

    public void updateCategory(Category category) {
        this.category = category;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Category getCategory() { return category; }
    public LocalDate getDate() { return date; }
    public String getHistory() { return history; }
    public String getDescription() { return description; }
    public String getNormalizedDescription() { return normalizedDescription; }
    public BigDecimal getAmount() { return amount; }
    public TransactionType getType() { return type; }
    public boolean isNeedsReview() { return needsReview; }
}
