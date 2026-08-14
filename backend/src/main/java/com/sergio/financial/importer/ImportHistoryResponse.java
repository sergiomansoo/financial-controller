package com.sergio.financial.importer;

import java.time.Instant;

public record ImportHistoryResponse(String originalFilename, Instant importedAt, int rowCount) {
    public static ImportHistoryResponse from(ImportHistory history) {
        return new ImportHistoryResponse(history.getOriginalFilename(), history.getImportedAt(), history.getRowCount());
    }
}
