package com.sergio.financial.importer;

import com.sergio.financial.transaction.TransactionResponse;
import java.util.List;

public record ImportResponse(int importedCount, int duplicateCount, List<TransactionResponse> transactions) {
}
