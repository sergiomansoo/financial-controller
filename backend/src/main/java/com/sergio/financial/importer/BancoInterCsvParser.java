package com.sergio.financial.importer;

import java.io.BufferedReader;
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
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class BancoInterCsvParser {
    private static final String HEADER = "Data Lançamento;Histórico;Descrição;Valor;Saldo";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern DECIMAL_COMMA = Pattern.compile("-?(?:\\d+|\\d{1,3}(?:\\.\\d{3})+),\\d{2}");

    public List<ParsedTransaction> parse(InputStream input) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            findHeader(reader);
            List<ParsedTransaction> transactions = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    transactions.add(parseRow(line));
                }
            }
            return transactions;
        } catch (IOException | DateTimeParseException | NumberFormatException exception) {
            throw new UnsupportedStatementFormatException();
        }
    }

    private void findHeader(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            if (HEADER.equals(line)) {
                return;
            }
            if (line.contains(";")) {
                throw new UnsupportedStatementFormatException();
            }
        }
        throw new UnsupportedStatementFormatException();
    }

    private ParsedTransaction parseRow(String line) {
        String[] columns = line.split(";", -1);
        if (columns.length != 5) {
            throw new UnsupportedStatementFormatException();
        }

        LocalDate date = LocalDate.parse(columns[0].trim(), DATE_FORMAT);
        String history = columns[1].trim();
        String description = columns[2].trim();
        if (description.isEmpty()) {
            description = null;
        }
        BigDecimal amount = parseDecimal(columns[3]);
        BigDecimal balance = parseDecimal(columns[4]);
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
