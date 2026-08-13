package com.sergio.financial.dashboard;

import com.sergio.financial.budget.BudgetResponse;
import com.sergio.financial.budget.BudgetService;
import com.sergio.financial.transaction.CategoryExpense;
import com.sergio.financial.transaction.FinancialTransaction;
import com.sergio.financial.transaction.FinancialTransactionRepository;
import com.sergio.financial.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final FinancialTransactionRepository transactions;
    private final BudgetService budgets;

    public DashboardService(FinancialTransactionRepository transactions, BudgetService budgets) {
        this.transactions = transactions;
        this.budgets = budgets;
    }

    public DashboardResponse dashboard(Long userId, YearMonth month) {
        List<CategoryExpense> expenses = budgets.categoryExpenses(userId, month);
        Map<Long, BigDecimal> expensesByCategory = expenses.stream()
                .collect(Collectors.toMap(CategoryExpense::categoryId, CategoryExpense::spent));

        return new DashboardResponse(
                expenses.stream()
                        .map(expense -> new CategorySpend(expense.categoryId(), expense.categoryName(), expense.spent()))
                        .toList(),
                monthlyEvolution(userId, month),
                budgets.list(userId, month, expensesByCategory));
    }

    private List<MonthlyEvolution> monthlyEvolution(Long userId, YearMonth month) {
        YearMonth firstMonth = month.minusMonths(5);
        Map<YearMonth, Totals> totalsByMonth = new TreeMap<>();
        for (YearMonth current = firstMonth; !current.isAfter(month); current = current.plusMonths(1)) {
            totalsByMonth.put(current, new Totals(BigDecimal.ZERO, BigDecimal.ZERO));
        }

        for (FinancialTransaction transaction : transactions
                .findByUserIdAndDateGreaterThanEqualAndDateLessThanOrderByDateDescIdDesc(
                        userId, firstMonth.atDay(1), month.plusMonths(1).atDay(1))) {
            YearMonth transactionMonth = YearMonth.from(transaction.getDate());
            Totals current = totalsByMonth.get(transactionMonth);
            if (transaction.getType() == TransactionType.INCOME) {
                totalsByMonth.put(transactionMonth, new Totals(current.income().add(transaction.getAmount()), current.expense()));
            } else if (transaction.getType() == TransactionType.EXPENSE) {
                totalsByMonth.put(transactionMonth, new Totals(current.income(), current.expense().add(transaction.getAmount())));
            }
        }

        return totalsByMonth.entrySet().stream()
                .map(entry -> new MonthlyEvolution(entry.getKey().toString(), entry.getValue().income(), entry.getValue().expense()))
                .toList();
    }

    public record CategorySpend(Long categoryId, String categoryName, BigDecimal spent) {
    }

    public record MonthlyEvolution(String month, BigDecimal income, BigDecimal expense) {
    }

    private record Totals(BigDecimal income, BigDecimal expense) {
    }

    public record DashboardResponse(List<CategorySpend> byCategory, List<MonthlyEvolution> monthlyEvolution,
                                    List<BudgetResponse> budgets) {
    }
}
