package com.sergio.financial.category;

import com.sergio.financial.budget.BudgetRepository;
import com.sergio.financial.rule.CategoryRuleRepository;
import com.sergio.financial.transaction.FinancialTransactionRepository;
import com.sergio.financial.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {
    private final CategoryRepository categories;
    private final UserRepository users;
    private final FinancialTransactionRepository transactions;
    private final CategoryRuleRepository rules;
    private final BudgetRepository budgets;

    public CategoryService(CategoryRepository categories, UserRepository users, FinancialTransactionRepository transactions,
                           CategoryRuleRepository rules, BudgetRepository budgets) {
        this.categories = categories;
        this.users = users;
        this.transactions = transactions;
        this.rules = rules;
        this.budgets = budgets;
    }

    @Transactional
    public CategoryResponse create(Long userId, CategoryRequest request) {
        Category category = categories.save(new Category(users.getReferenceById(userId), request.name().trim()));
        return new CategoryResponse(category.getId(), category.getName());
    }

    @Transactional
    public void delete(Long userId, Long categoryId) {
        Category category = categories.findById(categoryId).orElseThrow(CategoryNotFoundException::new);
        if (category.isSystemCategory()) {
            throw new CategoryNotDeletableException();
        }
        if (!category.getUser().getId().equals(userId)) {
            throw new CategoryNotFoundException();
        }
        if (transactions.existsByCategoryId(categoryId) || rules.existsByCategoryId(categoryId)
                || budgets.existsByCategoryId(categoryId)) {
            throw new CategoryInUseException();
        }
        categories.delete(category);
    }
}
