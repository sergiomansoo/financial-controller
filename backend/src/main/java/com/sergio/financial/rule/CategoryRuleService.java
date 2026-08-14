package com.sergio.financial.rule;

import com.sergio.financial.category.Category;
import com.sergio.financial.category.CategoryRepository;
import com.sergio.financial.category.CategoryResponse;
import com.sergio.financial.transaction.TransactionNotFoundException;
import com.sergio.financial.transaction.FinancialTransactionRepository;
import com.sergio.financial.user.User;
import com.sergio.financial.user.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryRuleService {
    private final CategoryRuleRepository rules;
    private final CategoryRepository categories;
    private final UserRepository users;
    private final CategorizationService categorization;
    private final FinancialTransactionRepository transactions;

    public CategoryRuleService(CategoryRuleRepository rules, CategoryRepository categories, UserRepository users,
                               CategorizationService categorization, FinancialTransactionRepository transactions) {
        this.rules = rules;
        this.categories = categories;
        this.users = users;
        this.categorization = categorization;
        this.transactions = transactions;
    }

    @Transactional(readOnly = true)
    public List<CategoryRuleResponse> list(Long userId) {
        return rules.findByUserIdOrderByIdDesc(userId).stream().map(this::response).toList();
    }

    @Transactional
    public CategoryRuleResponse create(Long userId, CategoryRuleRequest request) {
        User user = users.getReferenceById(userId);
        Category category = categories.findAccessibleByIdAndUserId(request.categoryId(), userId)
                .orElseThrow(TransactionNotFoundException::new);
        String keyword = categorization.normalize(request.keyword());
        CategoryRule rule = rules.findByUserIdAndNormalizedDescription(userId, keyword)
                .map(existing -> { existing.updateCategory(category); return existing; })
                .orElseGet(() -> rules.save(new CategoryRule(user, category, keyword)));
        return response(rule);
    }

    @Transactional
    public void delete(Long userId, Long ruleId) {
        CategoryRule rule = rules.findByIdAndUserId(ruleId, userId).orElseThrow(CategoryRuleNotFoundException::new);
        rules.delete(rule);
    }

    @Transactional
    public CategoryRuleApplyResponse apply(Long userId, Long ruleId) {
        CategoryRule rule = rules.findByIdAndUserId(ruleId, userId).orElseThrow(CategoryRuleNotFoundException::new);
        int changedCount = transactions.reclassifyByNormalizedDescriptionPrefix(userId,
                categorization.normalize(rule.getNormalizedDescription()), rule.getCategory(), rule.getCategory().getId());
        return new CategoryRuleApplyResponse(changedCount);
    }

    private CategoryRuleResponse response(CategoryRule rule) {
        Category category = rule.getCategory();
        return new CategoryRuleResponse(rule.getId(), rule.getNormalizedDescription(),
                new CategoryResponse(category.getId(), category.getName(), category.isSalary()));
    }
}
