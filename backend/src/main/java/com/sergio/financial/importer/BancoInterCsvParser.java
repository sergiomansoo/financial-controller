package com.sergio.financial.importer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

@Component
public class BancoInterCsvParser {
    private static final List<String> HEADER = List.of(
            "Data Lançamento", "Histórico", "Descrição", "Valor", "Saldo");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern DECIMAL_COMMA = Pattern.compile("-?(?:\\d+|\\d{1,3}(?:\\.\\d{3})+),\\d{2}");
    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
            .setDelimiter(';')
            .setQuote('"')
            .setIgnoreEmptyLines(false)
            .build();

    public List<ParsedTransaction> parse(InputStream input) {
        try (CSVParser csv = CSVParser.parse(new InputStreamReader(input, StandardCharsets.UTF_8), CSV_FORMAT)) {
            List<CSVRecord> records = csv.getRecords();
            validateMetadata(records);
            return records.subList(6, records.size()).stream()
                    .filter(record -> !isBlank(record))
                    .map(this::parseRow)
                    .toList();
        } catch (IOException | DateTimeParseException | NumberFormatException exception) {
            throw new UnsupportedStatementFormatException();
        }
    }

    private void validateMetadata(List<CSVRecord> records) {
        if (records.size() < 6
                || !title(records.get(0)).equals("Extrato Conta Corrente")
                || !metadataValue(records.get(1), "Conta")
                || !metadataValue(records.get(2), "Período")
                || !metadataValue(records.get(3), "Saldo")
                || !isBlank(records.get(4))
                || !headerMatches(records.get(5))) {
            throw new UnsupportedStatementFormatException();
        }
    }

    private String title(CSVRecord record) {
        if (record.size() != 1) {
            return "";
        }
        String value = record.get(0).trim();
        return value.startsWith("\uFEFF") ? value.substring(1).trim() : value;
    }

    private boolean metadataValue(CSVRecord record, String key) {
        return record.size() == 2
                && record.get(0).trim().equals(key)
                && !record.get(1).trim().isEmpty();
    }

    private boolean isBlank(CSVRecord record) {
        return record.size() == 1 && record.get(0).isBlank();
    }

    private boolean headerMatches(CSVRecord record) {
        return record.size() == HEADER.size()
                && HEADER.equals(record.stream().map(String::trim).toList());
    }

    private ParsedTransaction parseRow(CSVRecord record) {
        if (record.size() != HEADER.size()) {
            throw new UnsupportedStatementFormatException();
        }
        LocalDate date = LocalDate.parse(record.get(0).trim(), DATE_FORMAT);
        String history = record.get(1).trim();
        String description = record.get(2).trim();
        if (description.isEmpty()) {
            description = null;
        }
        BigDecimal amount = parseDecimal(record.get(3));
        BigDecimal balance = parseDecimal(record.get(4));
        return new ParsedTransaction(date, history, description, amount, balance,
                fingerprint(date, history, description, amount));
    }

    private BigDecimal parseDecimal(String value) {
        String decimal = value.trim();
        if (!DECIMAL_COMMA.matcher(decimal).matches()) {
            throw new UnsupportedStatementFormatException();
        }
        return new BigDecimal(decimal.replace(".", "").replace(',', '.'));
    }

    private String fingerprint(LocalDate date, String history, String description, BigDecimal amount) {
        String source = String.join("|", date.toString(), history, description == null ? "" : description,
                amount.stripTrailingZeros().toPlainString());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
