package com.sergio.financial.rule;

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

@Entity
@Table(name = "category_rules")
public class CategoryRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "normalized_description", nullable = false)
    private String normalizedDescription;

    protected CategoryRule() {
    }

    public CategoryRule(User user, Category category, String normalizedDescription) {
        this.user = user;
        this.category = category;
        this.normalizedDescription = normalizedDescription;
    }

    public void updateCategory(Category category) {
        this.category = category;
    }

    public Long getId() { return id; }
    public Category getCategory() { return category; }
    public String getNormalizedDescription() { return normalizedDescription; }
}
