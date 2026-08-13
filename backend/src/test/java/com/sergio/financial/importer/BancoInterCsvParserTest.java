package com.sergio.financial.importer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BancoInterCsvParserTest {
    private final BancoInterCsvParser parser = new BancoInterCsvParser();

    @Test
    void parsesNegativeCommaDecimalAndTrimsHistory() {
        ParsedTransaction row = parser.parse(fixture("inter-valid.csv")).getFirst();

        assertThat(row.date()).isEqualTo(LocalDate.of(2026, 7, 15));
        assertThat(row.history()).isEqualTo("Pix enviado");
        assertThat(row.description()).isEqualTo("Transfer\u00eancia fict\u00edcia");
        assertThat(row.amount()).isEqualByComparingTo(new BigDecimal("-45.90"));
        assertThat(row.balance()).isEqualByComparingTo(new BigDecimal("1234.56"));
    }

    @Test
    void mapsEmptyDescriptionToNull() {
        ParsedTransaction row = parser.parse(fixture("inter-empty-description.csv")).getFirst();

        assertThat(row.history()).isEqualTo("Compra fict\u00edcia");
        assertThat(row.description()).isNull();
        assertThat(row.amount()).isEqualByComparingTo("12.50");
    }

    @Test
    void producesEqualFingerprintsForEquivalentDuplicateCandidates() {
        List<ParsedTransaction> rows = parser.parse(bytes(statement("""
                18/07/2026;  Fictional payment  ;Fictional service;-20,00;957,50
                18/07/2026;Fictional payment;Fictional service;-20,00;937,50
                """)));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).duplicateFingerprint()).isEqualTo(rows.get(1).duplicateFingerprint());
    }

    @Test
    void rejectsAnInvalidHeaderWithTheDocumentedMessage() {
        assertUnsupported(fixture("inter-invalid-header.csv"));
    }

    @Test
    void rejectsDotDecimalValues() {
        assertUnsupported(bytes(statement("18/07/2026;Fictional row;Description;1.23;10,00")));
    }

    private String statement(String rows) {
        return """
                Extrato Conta Corrente
                Conta ;000001-1
                Per\u00edodo ;01/07/2026 a 31/07/2026
                Saldo ;1.000,00

                Data Lan\u00e7amento;Hist\u00f3rico;Descri\u00e7\u00e3o;Valor;Saldo
                %s
                """.formatted(rows);
    }

    private void assertUnsupported(InputStream input) {
        assertThatThrownBy(() -> parser.parse(input))
                .isInstanceOf(UnsupportedStatementFormatException.class)
                .hasMessage("Formato de extrato n\u00e3o suportado. Envie um CSV Banco Inter em UTF-8.");
    }

    private ByteArrayInputStream bytes(String statement) {
        return new ByteArrayInputStream(statement.getBytes(StandardCharsets.UTF_8));
    }

    private InputStream fixture(String name) {
        InputStream input = getClass().getResourceAsStream("/fixtures/" + name);
        assertThat(input).as("fixture %s", name).isNotNull();
        return input;
    }
}
