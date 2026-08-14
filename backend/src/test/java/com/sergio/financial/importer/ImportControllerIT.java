package com.sergio.financial.importer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ImportControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void importsStatementAndRetainsDuplicateCandidateForReview() throws Exception {
        String token = register("Import User", "import.user@example.test");

        mockMvc.perform(importStatement(token, statement("""
                15/07/2026;Compra;Padaria Exemplo;-18,50;981,50
                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedCount").value(1))
                .andExpect(jsonPath("$.duplicateCount").value(0))
                .andExpect(jsonPath("$.transactions[0].description").value("Padaria Exemplo"))
                .andExpect(jsonPath("$.transactions[0].category.name").value("Alimenta\u00e7\u00e3o"))
                .andExpect(jsonPath("$.transactions[0].needsReview").value(false));

        mockMvc.perform(importStatement(token, statement("""
                15/07/2026;Compra;Padaria Exemplo;-18,50;963,00
                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedCount").value(1))
                .andExpect(jsonPath("$.duplicateCount").value(1))
                .andExpect(jsonPath("$.transactions[0].needsReview").value(true));

        mockMvc.perform(get("/api/v1/transactions").param("month", "2026-07")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].needsReview").value(true));
    }

    @Test
    void recordsSuccessfulImportsForTheAuthenticatedUserInReverseChronologicalOrder() throws Exception {
        String token = register("Import History User", "import.history@example.test");

        mockMvc.perform(importStatement("first.csv", token, statement("""
                15/07/2026;Compra;Primeira Compra;-18,50;981,50
                """)))
                .andExpect(status().isOk());
        mockMvc.perform(importStatement("latest.csv", token, statement("""
                16/07/2026;Compra;Compra Mais Recente;-22,00;959,50
                """)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/imports").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].originalFilename").value("latest.csv"))
                .andExpect(jsonPath("$[0].importedAt").isNotEmpty())
                .andExpect(jsonPath("$[0].rowCount").value(1))
                .andExpect(jsonPath("$[1].originalFilename").value("first.csv"))
                .andExpect(jsonPath("$[1].rowCount").value(1));
    }

    @Test
    void doesNotRecordFailedImports() throws Exception {
        String token = register("Failed Import History", "failed.import.history@example.test");
        MockMultipartFile file = new MockMultipartFile("file", "broken.csv", "text/csv",
                "not a Banco Inter statement".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/imports").file(file).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/imports").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void rejectsInvalidStatementFormatWithStandardErrorBody() throws Exception {
        String token = register("Invalid Import", "invalid.import@example.test");
        MockMultipartFile file = new MockMultipartFile("file", "not-inter.csv", "text/csv",
                "not a Banco Inter statement".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/imports").file(file).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_STATEMENT_FORMAT"))
                .andExpect(jsonPath("$.message").value("Formato de extrato n\u00e3o suportado. Envie um CSV Banco Inter em UTF-8."));
    }

    @Test
    void learnedDescriptionRuleTakesPriorityOverSystemKeywordRule() throws Exception {
        String token = register("Rules User", "rules.user@example.test");
        JsonNode categories = getCategories(token);
        long transportId = categoryId(categories, "Transporte");

        JsonNode first = importAndRead(token, statement("""
                16/07/2026;Compra;Ifood Exemplo;-22,00;978,00
                """));
        long transactionId = first.path("transactions").get(0).path("id").asLong();

        mockMvc.perform(patch("/api/v1/transactions/{id}/category", transactionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":%d,\"learn\":true}".formatted(transportId)))
                .andExpect(status().isOk());

        mockMvc.perform(importStatement(token, statement("""
                17/07/2026;Compra;  ifood exemplo  ;-25,00;953,00
                """)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions[0].category.name").value("Transporte"));
    }

    @Test
    void previewsStatementWithoutPersistingTransactions() throws Exception {
        String token = register("Preview User", "preview.user@example.test");

        mockMvc.perform(multipart("/api/v1/imports/preview")
                        .file(new MockMultipartFile("file", "statement.csv", "text/csv", statement("""
                                15/07/2026;Compra;Padaria Exemplo;-18,50;981,50
                                """).getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.previewCount").value(1))
                .andExpect(jsonPath("$.rows[0].description").value("Padaria Exemplo"));

        mockMvc.perform(get("/api/v1/transactions").param("month", "2026-07")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private JsonNode importAndRead(String token, String csv) throws Exception {
        MvcResult result = mockMvc.perform(importStatement(token, csv)).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder importStatement(String token, String csv) {
        return importStatement("statement.csv", token, csv);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder importStatement(String filename, String token, String csv) {
        MockMultipartFile file = new MockMultipartFile("file", filename, "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));
        return multipart("/api/v1/imports").file(file).header("Authorization", "Bearer " + token);
    }

    private JsonNode getCategories(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/categories").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private long categoryId(JsonNode categories, String name) {
        for (JsonNode category : categories) {
            if (name.equals(category.path("name").asText())) {
                return category.path("id").asLong();
            }
        }
        throw new AssertionError("Category not found: " + name);
    }

    private String register(String name, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"email\":\"%s\",\"password\":\"FictionalPassword1!\"}".formatted(name, email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("accessToken").asText();
    }

    private String statement(String rows) {
        return """
                Extrato Conta Corrente
                Conta ;000001-1
                Período ;01/07/2026 a 31/07/2026
                Saldo ;1.000,00

                Data Lançamento;Histórico;Descrição;Valor;Saldo
                %s
                """.formatted(rows);
    }
}
