package com.sergio.financial.transaction;

import com.sergio.financial.category.Category;
import com.sergio.financial.category.CategoryRepository;
import com.sergio.financial.category.CategoryResponse;
import com.sergio.financial.rule.CategorizationService;
import com.sergio.financial.user.User;
import com.sergio.financial.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {
    private final FinancialTransactionRepository transactions;
    private final CategoryRepository categories;
    private final UserRepository users;
    private final CategorizationService categorization;

    public TransactionService(FinancialTransactionRepository transactions, CategoryRepository categories,
                              UserRepository users, CategorizationService categorization) {
        this.transactions = transactions;
        this.categories = categories;
        this.users = users;
        this.categorization = categorization;
    }

    @Transactional
    public TransactionResponse createManual(Long userId, ManualTransactionRequest request) {
        User user = user(userId);
        Category category = accessibleCategory(request.categoryId(), userId);
        String description = request.description().trim();
        FinancialTransaction transaction = transactions.save(new FinancialTransaction(
                user, category, request.date(), null, description, categorization.normalize(description),
                request.amount(), request.type(), null, false));
        return response(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> list(Long userId, YearMonth month) {
        return transactions.findByUserIdAndDateGreaterThanEqualAndDateLessThanOrderByDateDescIdDesc(
                        userId, month.atDay(1), month.plusMonths(1).atDay(1))
                .stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public TransactionPageResponse page(Long userId, YearMonth month, TransactionType type, Long categoryId,
                                        LocalDate fromDate, LocalDate toDate, int page, int size) {
        DateRange range = dateRange(month, fromDate, toDate);
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        Pageable pageable = PageRequest.of(safePage, safeSize);
        return TransactionPageResponse.from(transactions.search(userId, range.from(), range.until(),
                type, categoryId, null, null, pageable).map(this::response));
    }

    @Transactional(readOnly = true)
    public TransactionTotalResponse total(Long userId, YearMonth month, TransactionType type, Long categoryId,
                                          LocalDate fromDate, LocalDate toDate) {
        DateRange range = dateRange(month, fromDate, toDate);
        BigDecimal total = transactions.sumFiltered(userId, range.from(), range.until(), type, categoryId);
        BigDecimal totalSpent = transactions.sumSpent(userId, range.from(), range.until(), type, categoryId);
        return new TransactionTotalResponse(total == null ? BigDecimal.ZERO : total,
                totalSpent == null ? BigDecimal.ZERO : totalSpent);
    }

    @Transactional
    public TransactionResponse updateCategory(Long userId, Long transactionId, CategoryUpdateRequest request) {
        FinancialTransaction transaction = transactions.findByIdAndUserId(transactionId, userId)
                .orElseThrow(TransactionNotFoundException::new);
        Category category = accessibleCategory(request.categoryId(), userId);
        transaction.updateCategory(category);
        if (request.learn()) {
            categorization.learn(transaction.getUser(), transaction.getNormalizedDescription(), category);
        }
        return response(transaction);
    }

    @Transactional
    public ImportedTransaction saveImported(Long userId, LocalDate date, String history, String description,
                                            BigDecimal amount, String fingerprint) {
        User user = user(userId);
        boolean duplicate = transactions.existsByUserIdAndDuplicateFingerprint(userId, fingerprint);
        String normalizedDescription = categorization.normalize(description == null ? history : description);
        Category category = categorization.categorize(user, normalizedDescription);
        FinancialTransaction transaction = transactions.save(new FinancialTransaction(
                user, category, date, history, description, normalizedDescription, amount, typeFor(history),
                fingerprint, duplicate));
        return new ImportedTransaction(response(transaction), duplicate);
    }

    @Transactional(readOnly = true)
    public boolean isDuplicate(Long userId, String fingerprint) {
        return transactions.existsByUserIdAndDuplicateFingerprint(userId, fingerprint);
    }

    public TransactionType typeForPreview(String history) {
        return typeFor(history);
    }

    private User user(Long userId) {
        return users.findById(userId).orElseThrow(TransactionNotFoundException::new);
    }

    private Category accessibleCategory(Long categoryId, Long userId) {
        return categories.findAccessibleByIdAndUserId(categoryId, userId)
                .orElseThrow(TransactionNotFoundException::new);
    }

    private DateRange dateRange(YearMonth month, LocalDate fromDate, LocalDate toDate) {
        if (month != null && (fromDate != null || toDate != null)) {
            throw new InvalidTransactionFilterException("Não combine mês com De/Até.",
                    java.util.Map.of("month", "Não combine mês com De/Até."));
        }
        if (month != null) {
            return new DateRange(month.atDay(1), month.plusMonths(1).atDay(1));
        }
        if (fromDate == null || toDate == null) {
            throw new InvalidTransactionFilterException("Informe as datas De e Até juntas.",
                    java.util.Map.of(fromDate == null ? "from" : "to", "Informe as datas De e Até juntas."));
        }
        if (fromDate.isAfter(toDate)) {
            throw new InvalidTransactionFilterException("A data De deve ser anterior ou igual a Até.",
                    java.util.Map.of("to", "A data De deve ser anterior ou igual a Até."));
        }
        return new DateRange(fromDate, toDate.plusDays(1));
    }

    private TransactionType typeFor(String history) {
        String normalized = categorization.normalize(history);
        if (normalized.contains("aplica\u00e7\u00e3o") || normalized.contains("resgate") || normalized.contains("cdb")) {
            return TransactionType.INVESTMENT;
        }
        if (normalized.contains("recebido") || normalized.contains("recebimento")
                || normalized.contains("cr\u00e9dito") || normalized.contains("credito") || normalized.contains("rendimento")) {
            return TransactionType.INCOME;
        }
        return TransactionType.EXPENSE;
    }

    private TransactionResponse response(FinancialTransaction transaction) {
        Category category = transaction.getCategory();
        return new TransactionResponse(transaction.getId(), transaction.getDate(), transaction.getHistory(),
                transaction.getDescription(), transaction.getAmount(), transaction.getType(),
                new CategoryResponse(category.getId(), category.getName(), category.isSalary()), transaction.isNeedsReview());
    }

    public record ImportedTransaction(TransactionResponse transaction, boolean duplicate) {
    }

    private record DateRange(LocalDate from, LocalDate until) {
    }
}
