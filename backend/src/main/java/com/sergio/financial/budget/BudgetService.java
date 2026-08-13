package com.sergio.financial.budget;

import com.sergio.financial.category.Category;
import com.sergio.financial.category.CategoryRepository;
import com.sergio.financial.transaction.CategoryExpense;
import com.sergio.financial.transaction.FinancialTransactionRepository;
import com.sergio.financial.transaction.TransactionNotFoundException;
import com.sergio.financial.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetService {
    private final BudgetRepository budgets;
    private final CategoryRepository categories;
    private final UserRepository users;
    private final FinancialTransactionRepository transactions;

    public BudgetService(BudgetRepository budgets, CategoryRepository categories, UserRepository users,
                         FinancialTransactionRepository transactions) {
        this.budgets = budgets;
        this.categories = categories;
        this.users = users;
        this.transactions = transactions;
    }

    @Transactional
    public BudgetResponse upsert(Long userId, Long categoryId, YearMonth month, BudgetRequest request) {
        Category category = categories.findAccessibleByIdAndUserId(categoryId, userId)
                .orElseThrow(TransactionNotFoundException::new);
        LocalDate budgetMonth = month.atDay(1);
        Budget budget = budgets.findByUserIdAndCategoryIdAndMonth(userId, categoryId, budgetMonth)
                .orElseGet(() -> new Budget(users.getReferenceById(userId), category, budgetMonth, request.limit()));
        budget.setLimit(request.limit());
        budgets.save(budget);
        return response(budget, monthlyExpenses(userId, month));
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> list(Long userId, YearMonth month) {
        return list(userId, month, monthlyExpenses(userId, month));
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> list(Long userId, YearMonth month, Map<Long, BigDecimal> expensesByCategory) {
        return budgets.findByUserIdAndMonth(userId, month.atDay(1)).stream()
                .map(budget -> response(budget, expensesByCategory))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryExpense> categoryExpenses(Long userId, YearMonth month) {
        return transactions.sumExpensesByCategory(userId, month.atDay(1), month.plusMonths(1).atDay(1));
    }

    public Map<Long, BigDecimal> monthlyExpenses(Long userId, YearMonth month) {
        return categoryExpenses(userId, month).stream()
                .collect(Collectors.toMap(CategoryExpense::categoryId, CategoryExpense::spent));
    }

    private BudgetResponse response(Budget budget, Map<Long, BigDecimal> expensesByCategory) {
        BigDecimal spent = expensesByCategory.getOrDefault(budget.getCategory().getId(), BigDecimal.ZERO);
        return new BudgetResponse(budget.getCategory().getId(), budget.getCategory().getName(), spent,
                budget.getLimit(), spent.compareTo(budget.getLimit()) > 0);
    }
}
