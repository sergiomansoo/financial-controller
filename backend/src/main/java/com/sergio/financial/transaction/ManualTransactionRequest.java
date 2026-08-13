package com.sergio.financial.transaction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ManualTransactionRequest(
        @NotNull LocalDate date,
        @NotBlank @Size(max = 255) String description,
        @NotNull BigDecimal amount,
        @NotNull Long categoryId,
        @NotNull TransactionType type) {
}
