package com.sergio.financial.importer;

import com.sergio.financial.transaction.TransactionResponse;
import com.sergio.financial.transaction.TransactionService;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/imports")
public class ImportController {
    private final BancoInterCsvParser parser;
    private final TransactionService transactions;
    private final ImportHistoryService history;

    public ImportController(BancoInterCsvParser parser, TransactionService transactions, ImportHistoryService history) {
        this.parser = parser;
        this.transactions = transactions;
        this.history = history;
    }

    @PostMapping
    public ImportResponse importStatement(@RequestParam("file") MultipartFile file, Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        try {
            List<TransactionService.ImportedTransaction> imported = parser.parse(file.getInputStream()).stream()
                    .map(row -> transactions.saveImported(userId, row.date(), row.history(), row.description(),
                            row.amount(), row.duplicateFingerprint()))
                    .toList();
            ImportResponse response = new ImportResponse(imported.size(),
                    (int) imported.stream().filter(TransactionService.ImportedTransaction::duplicate).count(),
                    imported.stream().map(TransactionService.ImportedTransaction::transaction).toList());
            history.record(userId, file.getOriginalFilename(), imported.size());
            return response;
        } catch (IOException exception) {
            throw new UnsupportedStatementFormatException();
        }
    }

    @GetMapping
    public List<ImportHistoryResponse> list(Authentication authentication) {
        return history.list(Long.valueOf(authentication.getName()));
    }

    @PostMapping("/preview")
    public ImportPreviewResponse previewStatement(@RequestParam("file") MultipartFile file, Authentication authentication) {
        Long userId = Long.valueOf(authentication.getName());
        try {
            List<ParsedTransaction> parsed = parser.parse(file.getInputStream());
            Set<String> duplicateFingerprints = transactions.duplicateFingerprints(userId,
                    parsed.stream().map(ParsedTransaction::duplicateFingerprint).toList());
            List<ImportPreviewResponse.PreviewRow> rows = parsed.stream()
                    .map(row -> new ImportPreviewResponse.PreviewRow(row.date(), row.history(), row.description(), row.amount(),
                            transactions.typeForPreview(row.history()), duplicateFingerprints.contains(row.duplicateFingerprint())))
                    .toList();
            return new ImportPreviewResponse(rows.size(), (int) rows.stream().filter(ImportPreviewResponse.PreviewRow::duplicate).count(), rows);
        } catch (IOException exception) {
            throw new UnsupportedStatementFormatException();
        }
    }
}
