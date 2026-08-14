package com.sergio.financial.transaction;

import jakarta.validation.Valid;
import java.time.YearMonth;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {
    private final TransactionService transactions;

    public TransactionController(TransactionService transactions) {
        this.transactions = transactions;
    }

    @GetMapping
    public Object list(@RequestParam(required = false) YearMonth month,
                       @RequestParam(required = false) TransactionType type,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) LocalDate from,
                       @RequestParam(required = false) LocalDate to,
                       @RequestParam(required = false) Integer page,
                       @RequestParam(required = false) Integer size,
                       Authentication authentication) {
        if (page == null && size == null && type == null && categoryId == null && from == null && to == null) {
            return transactions.list(userId(authentication), month);
        }
        return transactions.page(userId(authentication), month, type, categoryId, from, to,
                page == null ? 0 : page, size == null ? 10 : size);
    }

    @GetMapping("/total")
    public TransactionTotalResponse total(@RequestParam(required = false) YearMonth month,
                                          @RequestParam(required = false) TransactionType type,
                                          @RequestParam(required = false) Long categoryId,
                                          @RequestParam(required = false) LocalDate from,
                                          @RequestParam(required = false) LocalDate to,
                                          Authentication authentication) {
        return transactions.total(userId(authentication), month, type, categoryId, from, to);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@Valid @RequestBody ManualTransactionRequest request, Authentication authentication) {
        return transactions.createManual(userId(authentication), request);
    }

    @PatchMapping("/{id}/category")
    public TransactionResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryUpdateRequest request,
                                              Authentication authentication) {
        return transactions.updateCategory(userId(authentication), id, request);
    }

    private Long userId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
