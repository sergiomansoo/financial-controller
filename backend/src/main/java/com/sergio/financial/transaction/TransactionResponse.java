package com.sergio.financial.transaction;

import com.sergio.financial.category.CategoryResponse;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(Long id, LocalDate date, String history, String description, BigDecimal amount,
                                  TransactionType type, CategoryResponse category, boolean needsReview) {
}
