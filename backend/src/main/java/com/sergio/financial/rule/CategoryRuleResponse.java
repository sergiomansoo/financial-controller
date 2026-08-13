package com.sergio.financial.rule;

import com.sergio.financial.category.CategoryResponse;

public record CategoryRuleResponse(Long id, String keyword, CategoryResponse category) {
}
