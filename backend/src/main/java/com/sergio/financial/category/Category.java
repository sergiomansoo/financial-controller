package com.sergio.financial.category;

import com.sergio.financial.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private boolean systemCategory;

    @Column(name = "is_salary", nullable = false)
    private boolean salary;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    protected Category() {
    }

    public Category(User user, String name, boolean salary) {
        this.user = user;
        this.name = name;
        this.salary = salary;
        this.systemCategory = false;
    }

    public static Category system(String name) {
        Category category = new Category();
        category.name = name;
        category.systemCategory = true;
        category.salary = false;
        return category;
    }

    public void updateSalary(boolean salary) {
        this.salary = salary;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public boolean isSystemCategory() { return systemCategory; }
    public boolean isSalary() { return salary; }
    public User getUser() { return user; }
}
