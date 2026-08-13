package com.sergio.financial.importer;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BancoInterCsvParserStructureTest {
    private final BancoInterCsvParser parser = new BancoInterCsvParser();

    @Test
    void parsesQuotedSemicolonAndMultilineDescription() {
        ParsedTransaction row = parser.parse(bytes(statement("""
                18/07/2026;Payment;"First line; with semicolon
                Second line";-20,00;957,50
                """))).getFirst();

        assertThat(row.description()).isEqualTo("First line; with semicolon\nSecond line");
    }

    @Test
    void acceptsAnOptionalUtf8BomBeforeTheTitle() {
        ParsedTransaction row = parser.parse(bytes("\uFEFF" + statement("18/07/2026;Payment;Description;-20,00;957,50")))
                .getFirst();

        assertThat(row.date()).isEqualTo(LocalDate.of(2026, 7, 18));
    }

    @Test
    void rejectsMissingOrInvalidMetadata() {
        assertUnsupported("""
                Extrato Conta Corrente
                Conta ;000001-1
                Per\u00edodo ;01/07/2026 a 31/07/2026
                Gerado;31/07/2026

                Data Lan\u00e7amento;Hist\u00f3rico;Descri\u00e7\u00e3o;Valor;Saldo
                18/07/2026;Payment;Description;-20,00;957,50
                """);
    }

    @Test
    void rejectsInvalidDatesAndWrongLogicalColumnCounts() {
        assertUnsupported(statement("31/02/2026;Payment;Description;-20,00;957,50"));
        assertUnsupported(statement("18/07/2026;Payment;Description;-20,00"));
    }

    private String statement(String records) {
        return """
                Extrato Conta Corrente
                Conta ;000001-1
                Per\u00edodo ;01/07/2026 a 31/07/2026
                Saldo ;1.000,00

                Data Lan\u00e7amento;Hist\u00f3rico;Descri\u00e7\u00e3o;Valor;Saldo
                %s
                """.formatted(records);
    }

    private void assertUnsupported(String statement) {
        assertThatThrownBy(() -> parser.parse(bytes(statement)))
                .isInstanceOf(UnsupportedStatementFormatException.class)
                .hasMessage("Formato de extrato n\u00e3o suportado. Envie um CSV Banco Inter em UTF-8.");
    }

    private ByteArrayInputStream bytes(String statement) {
        return new ByteArrayInputStream(statement.getBytes(StandardCharsets.UTF_8));
    }
}
