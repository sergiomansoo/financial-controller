package com.sergio.financial.budget;

import jakarta.validation.Valid;
import java.time.YearMonth;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {
    private final BudgetService service;

    public BudgetController(BudgetService service) {
        this.service = service;
    }

    @PutMapping("/{categoryId}")
    public BudgetResponse put(@PathVariable Long categoryId, @RequestParam YearMonth month,
                              @Valid @RequestBody BudgetRequest request, Authentication authentication) {
        return service.upsert(userId(authentication), categoryId, month, request);
    }

    @GetMapping
    public List<BudgetResponse> get(@RequestParam YearMonth month, Authentication authentication) {
        return service.list(userId(authentication), month);
    }

    private Long userId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
