package com.sergio.financial.category;

import com.sergio.financial.user.User;
import jakarta.persistence.Entity;
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

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    protected Category() {
    }

    public Category(User user, String name) {
        this.user = user;
        this.name = name;
        this.systemCategory = false;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public boolean isSystemCategory() { return systemCategory; }
    public User getUser() { return user; }
}
