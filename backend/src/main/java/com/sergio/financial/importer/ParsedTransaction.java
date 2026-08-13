package com.sergio.financial.importer;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParsedTransaction(
        LocalDate date,
        String history,
        String description,
        BigDecimal amount,
        BigDecimal balance,
        String duplicateFingerprint) {
}
