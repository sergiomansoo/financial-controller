package com.sergio.financial.importer;

import com.sergio.financial.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ImportPreviewResponse(int previewCount, int duplicateCount, List<PreviewRow> rows) {
    public record PreviewRow(LocalDate date, String history, String description, BigDecimal amount, TransactionType type,
                             boolean duplicate) {
    }
}
