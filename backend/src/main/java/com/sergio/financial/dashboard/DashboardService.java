package com.sergio.financial.dashboard;

import com.sergio.financial.budget.BudgetResponse;
import com.sergio.financial.budget.BudgetService;
import com.sergio.financial.transaction.CategoryExpense;
import com.sergio.financial.transaction.FinancialTransaction;
import com.sergio.financial.transaction.FinancialTransactionRepository;
import com.sergio.financial.transaction.TransactionType;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    public DashboardResponse dashboard(Long userId, YearMonth month, DashboardFilter filter) {
        List<CategoryExpense> expenses = budgets.categoryExpenses(userId, month);
        Map<Long, BigDecimal> expensesByCategory = expenses.stream()
                .collect(Collectors.toMap(CategoryExpense::categoryId, CategoryExpense::spent));

        return new DashboardResponse(
                totals(userId, month, filter, expenses),
                transactions.sumByCategory(userId, month.atDay(1), month.plusMonths(1).atDay(1), filter.transactionType()).stream()
                        .map(expense -> new CategorySpend(expense.categoryId(), expense.categoryName(), expense.spent()))
                        .toList(),
                monthlyEvolution(userId, month, filter),
                budgets.list(userId, month, expensesByCategory));
    }

    private TotalsResponse totals(Long userId, YearMonth month, DashboardFilter filter,
                                  List<CategoryExpense> expenses) {
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        BigDecimal expenseCommitted = BigDecimal.ZERO;
        BigDecimal salaryReceived = BigDecimal.ZERO;
        BigDecimal incomeReceived = BigDecimal.ZERO;
        BigDecimal receivedInvested = BigDecimal.ZERO;
        for (FinancialTransaction transaction : transactions
                .findByUserIdAndDateGreaterThanEqualAndDateLessThanOrderByDateDescIdDesc(
                        userId, month.atDay(1), month.plusMonths(1).atDay(1))) {
            if (transaction.getType() == TransactionType.INCOME) {
                incomeReceived = incomeReceived.add(transaction.getAmount());
                if (transaction.getCategory().isSalary()) {
                    salaryReceived = salaryReceived.add(transaction.getAmount());
                }
            } else if (transaction.getType() == TransactionType.EXPENSE) {
                expenseCommitted = expenseCommitted.add(transaction.getAmount().abs());
            } else if (transaction.getType() == TransactionType.INVESTMENT) {
                receivedInvested = receivedInvested.add(transaction.getAmount());
            }
            if (transaction.getType() == TransactionType.INCOME && filter.includes(TransactionType.INCOME)) {
                income = income.add(transaction.getAmount());
            }
            if (transaction.getType() == TransactionType.EXPENSE && filter.includes(TransactionType.EXPENSE)) {
                expense = expense.add(transaction.getAmount().abs());
            }
        }
        CategoryExpense largest = filter.includes(TransactionType.EXPENSE)
                ? expenses.stream().max(java.util.Comparator.comparing(CategoryExpense::spent)).orElse(null)
                : null;
        return new TotalsResponse(income.subtract(expense), income, expense,
                largest == null ? null : largest.categoryName(), largest == null ? BigDecimal.ZERO : largest.spent(),
                percentage(expenseCommitted, salaryReceived), percentage(receivedInvested, incomeReceived));
    }

    private BigDecimal percentage(BigDecimal amount, BigDecimal total) {
        if (total.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return amount.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }

    private List<MonthlyEvolution> monthlyEvolution(Long userId, YearMonth month, DashboardFilter filter) {
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
            if (transaction.getType() == TransactionType.INCOME && filter.includes(TransactionType.INCOME)) {
                totalsByMonth.put(transactionMonth, new Totals(current.income().add(transaction.getAmount()), current.expense()));
            } else if (transaction.getType() == TransactionType.EXPENSE && filter.includes(TransactionType.EXPENSE)) {
                totalsByMonth.put(transactionMonth,
                        new Totals(current.income(), current.expense().add(transaction.getAmount().abs())));
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

    public record TotalsResponse(BigDecimal balance, BigDecimal income, BigDecimal expense, String largestExpenseCategory,
                                 BigDecimal largestExpenseAmount, BigDecimal salaryCommittedPercent,
                                 BigDecimal receivedInvestedPercent) {
    }

    public record DashboardResponse(TotalsResponse totals, List<CategorySpend> byCategory, List<MonthlyEvolution> monthlyEvolution,
                                    List<BudgetResponse> budgets) {
    }
}
