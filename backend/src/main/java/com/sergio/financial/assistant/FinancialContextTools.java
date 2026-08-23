package com.sergio.financial.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sergio.financial.budget.BudgetService;
import com.sergio.financial.config.GroqProperties;
import com.sergio.financial.dashboard.DashboardService;
import com.sergio.financial.error.AiUnavailableException;
import com.sergio.financial.transaction.TransactionResponse;
import com.sergio.financial.transaction.TransactionService;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class FinancialContextTools {
    private static final int HARD_TRANSACTION_LIMIT = 50;

    private final TransactionService transactions;
    private final BudgetService budgets;
    private final DashboardService dashboards;
    private final GroqProperties properties;
    private final ObjectMapper objectMapper;

    public FinancialContextTools(TransactionService transactions, BudgetService budgets,
                                 DashboardService dashboards, GroqProperties properties,
                                 ObjectMapper objectMapper) {
        this.transactions = transactions;
        this.budgets = budgets;
        this.dashboards = dashboards;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public JsonNode execute(Long userId, YearMonth selectedMonth, String name, JsonNode arguments) {
        if (userId == null || selectedMonth == null || name == null) {
            throw new AiUnavailableException();
        }
        return switch (name) {
            case "get_monthly_dashboard" -> monthlyDashboard(userId, selectedMonth, arguments);
            case "get_monthly_budgets" -> monthlyBudgets(userId, selectedMonth, arguments);
            case "list_month_transactions" -> monthTransactions(userId, selectedMonth, arguments);
            case "compare_monthly_totals" -> compareMonthlyTotals(userId, selectedMonth, arguments);
            default -> throw new AiUnavailableException();
        };
    }

    private JsonNode monthlyDashboard(Long userId, YearMonth selectedMonth, JsonNode arguments) {
        YearMonth month = month(arguments, Set.of("month"), selectedMonth);
        return objectMapper.valueToTree(dashboards.dashboard(userId, month));
    }

    private JsonNode monthlyBudgets(Long userId, YearMonth selectedMonth, JsonNode arguments) {
        YearMonth month = month(arguments, Set.of("month"), selectedMonth);
        return objectMapper.valueToTree(budgets.list(userId, month));
    }

    private JsonNode monthTransactions(Long userId, YearMonth selectedMonth, JsonNode arguments) {
        YearMonth month = month(arguments, Set.of("month", "limit"), selectedMonth);
        JsonNode requestedLimit = arguments.get("limit");
        if (requestedLimit == null || !requestedLimit.isIntegralNumber()) {
            throw new AiUnavailableException();
        }
        int limit = Math.max(1, requestedLimit.asInt());
        int configuredLimit = Math.max(0, properties.transactionContextLimit());
        int effectiveLimit = Math.min(limit, Math.min(HARD_TRANSACTION_LIMIT, configuredLimit));
        List<TransactionContext> result = transactions.list(userId, month).stream()
                .limit(effectiveLimit)
                .map(this::transactionContext)
                .toList();
        return objectMapper.valueToTree(result);
    }

    private JsonNode compareMonthlyTotals(Long userId, YearMonth selectedMonth, JsonNode arguments) {
        YearMonth month = month(arguments, Set.of("month"), selectedMonth);
        YearMonth previousMonth = month.minusMonths(1);
        DashboardService.DashboardResponse currentDashboard = dashboards.dashboard(userId, month);
        DashboardService.DashboardResponse previousDashboard = dashboards.dashboard(userId, previousMonth);
        MonthTotals current = totals(currentDashboard, month);
        MonthTotals previous = totals(previousDashboard, previousMonth);
        return objectMapper.valueToTree(new MonthComparison(current, previous));
    }

    private MonthTotals totals(DashboardService.DashboardResponse dashboard, YearMonth month) {
        return dashboard.monthlyEvolution().stream()
                .filter(item -> month.toString().equals(item.month()))
                .findFirst()
                .map(item -> new MonthTotals(item.month(), item.income(), item.expense()))
                .orElseGet(() -> new MonthTotals(month.toString(), BigDecimal.ZERO, BigDecimal.ZERO));
    }

    private TransactionContext transactionContext(TransactionResponse transaction) {
        String description = transaction.description();
        if (description == null || description.isBlank()) {
            description = transaction.history();
        }
        String category = transaction.category() == null ? null : transaction.category().name();
        return new TransactionContext(transaction.date(), description, transaction.amount(),
                transaction.type(), category);
    }

    private YearMonth month(JsonNode arguments, Set<String> allowedFields, YearMonth selectedMonth) {
        if (arguments == null || !arguments.isObject()) {
            throw new AiUnavailableException();
        }
        java.util.Iterator<String> fields = arguments.fieldNames();
        while (fields.hasNext()) {
            if (!allowedFields.contains(fields.next())) {
                throw new AiUnavailableException();
            }
        }
        JsonNode month = arguments.get("month");
        if (month == null || !month.isTextual()) {
            throw new AiUnavailableException();
        }
        try {
            YearMonth requestedMonth = YearMonth.parse(month.textValue());
            if (!selectedMonth.equals(requestedMonth)) {
                throw new AiUnavailableException();
            }
            return requestedMonth;
        } catch (DateTimeParseException exception) {
            throw new AiUnavailableException();
        }
    }

    private record TransactionContext(java.time.LocalDate date, String description, BigDecimal amount,
                                      com.sergio.financial.transaction.TransactionType type, String category) {
    }

    private record MonthTotals(String month, BigDecimal income, BigDecimal expense) {
    }

    private record MonthComparison(MonthTotals current, MonthTotals previous) {
    }
}
