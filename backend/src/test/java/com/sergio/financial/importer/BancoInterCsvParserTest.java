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
        List<ParsedTransaction> rows = parser.parse(fixture("inter-valid.csv"));

        assertThat(rows).hasSize(1);
        ParsedTransaction row = rows.getFirst();
        assertThat(row.date()).isEqualTo(LocalDate.of(2026, 7, 15));
        assertThat(row.history()).isEqualTo("Pix enviado");
        assertThat(row.description()).isEqualTo("Transferência fictícia");
        assertThat(row.amount()).isEqualByComparingTo(new BigDecimal("-45.90"));
        assertThat(row.balance()).isEqualByComparingTo(new BigDecimal("1234.56"));
    }

    @Test
    void mapsEmptyDescriptionToNull() {
        List<ParsedTransaction> rows = parser.parse(fixture("inter-empty-description.csv"));

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.history()).isEqualTo("Compra fictícia");
            assertThat(row.description()).isNull();
            assertThat(row.amount()).isEqualByComparingTo("12.50");
        });
    }

    @Test
    void producesEqualFingerprintsForEquivalentDuplicateCandidates() {
        String statement = """
                Metadado fictício

                Data Lançamento;Histórico;Descrição;Valor;Saldo
                18/07/2026;  Pagamento fictício  ;Serviço fictício;-20,00;957,50
                18/07/2026;Pagamento fictício;Serviço fictício;-20,00;937,50
                """;

        List<ParsedTransaction> rows = parser.parse(
                new ByteArrayInputStream(statement.getBytes(StandardCharsets.UTF_8)));

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).duplicateFingerprint()).isEqualTo(rows.get(1).duplicateFingerprint());
    }

    @Test
    void rejectsAnInvalidHeaderWithTheDocumentedMessage() {
        assertThatThrownBy(() -> parser.parse(fixture("inter-invalid-header.csv")))
                .isInstanceOf(UnsupportedStatementFormatException.class)
                .hasMessage("Formato de extrato não suportado. Envie um CSV Banco Inter em UTF-8.");
    }

    @Test
    void rejectsTransactionDataBeforeTheHeader() {
        String statement = """
                Metadata
                17/07/2026;Transaction row;Description;1,00;10,00
                Data Lançamento;Histórico;Descrição;Valor;Saldo
                18/07/2026;Valid row;Description;1,00;11,00
                """;

        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(statement.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(UnsupportedStatementFormatException.class)
                .hasMessage("Formato de extrato não suportado. Envie um CSV Banco Inter em UTF-8.");
    }

    @Test
    void rejectsDotDecimalValues() {
        String statement = """
                Metadata
                Data Lançamento;Histórico;Descrição;Valor;Saldo
                18/07/2026;Fictional row;Description;1.23;10,00
                """;

        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(statement.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(UnsupportedStatementFormatException.class)
                .hasMessage("Formato de extrato não suportado. Envie um CSV Banco Inter em UTF-8.");
    }

    private InputStream fixture(String name) {
        InputStream input = getClass().getResourceAsStream("/fixtures/" + name);
        assertThat(input).as("fixture %s", name).isNotNull();
        return input;
    }
}
