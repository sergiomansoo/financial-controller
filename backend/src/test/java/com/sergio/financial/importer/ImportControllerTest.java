package com.sergio.financial.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sergio.financial.transaction.TransactionService;
import com.sergio.financial.transaction.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class ImportControllerTest {
    @Mock
    private BancoInterCsvParser parser;
    @Mock
    private TransactionService transactions;
    @Mock
    private ImportHistoryService history;
    @Mock
    private Authentication authentication;

    @Test
    void previewsDuplicatesWithOneBatchLookupInsteadOfOneQueryPerCsvRow() throws Exception {
        List<ParsedTransaction> rows = List.of(
                row("first"),
                row("second"),
                row("third"));
        when(authentication.getName()).thenReturn("42");
        when(parser.parse(any())).thenReturn(rows);
        when(transactions.duplicateFingerprints(42L, List.of("first", "second", "third")))
                .thenReturn(Set.of("second"));
        when(transactions.typeForPreview(any())).thenReturn(TransactionType.EXPENSE);
        ImportController controller = new ImportController(parser, transactions, history);

        ImportPreviewResponse response = controller.previewStatement(
                new MockMultipartFile("file", "statement.csv", "text/csv", "content".getBytes()), authentication);

        assertThat(response.previewCount()).isEqualTo(3);
        assertThat(response.duplicateCount()).isEqualTo(1);
        assertThat(response.rows()).extracting(ImportPreviewResponse.PreviewRow::duplicate)
                .containsExactly(false, true, false);
        verify(transactions).duplicateFingerprints(42L, List.of("first", "second", "third"));
        verify(transactions, never()).isDuplicate(any(), any());
    }

    private ParsedTransaction row(String fingerprint) {
        return new ParsedTransaction(LocalDate.of(2026, 8, 1), "Compra", fingerprint,
                BigDecimal.ONE, BigDecimal.ONE, fingerprint);
    }
}
