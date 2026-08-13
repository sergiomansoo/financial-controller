package com.sergio.financial.budget;

import java.math.BigDecimal;

public record BudgetResponse(Long categoryId, String categoryName, BigDecimal spent, BigDecimal limit,
                             boolean exceeded) {
}
