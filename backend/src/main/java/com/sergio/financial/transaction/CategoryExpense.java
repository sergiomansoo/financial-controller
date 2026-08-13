package com.sergio.financial.transaction;

import java.math.BigDecimal;

public record CategoryExpense(Long categoryId, String categoryName, BigDecimal spent) {
}
