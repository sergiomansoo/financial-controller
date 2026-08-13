package com.sergio.financial.rule;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/category-rules")
public class CategoryRuleController {
    private final CategoryRuleService service;

    public CategoryRuleController(CategoryRuleService service) {
        this.service = service;
    }

    @GetMapping
    public List<CategoryRuleResponse> list(Authentication authentication) {
        return service.list(Long.valueOf(authentication.getName()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryRuleResponse create(@Valid @RequestBody CategoryRuleRequest request, Authentication authentication) {
        return service.create(Long.valueOf(authentication.getName()), request);
    }

    @DeleteMapping("/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long ruleId, Authentication authentication) {
        service.delete(Long.valueOf(authentication.getName()), ruleId);
    }
}
