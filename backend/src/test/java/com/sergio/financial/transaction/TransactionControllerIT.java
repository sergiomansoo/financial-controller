package com.sergio.financial.transaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsManualTransactionAndListsOnlyTransactionsForRequestedMonth() throws Exception {
        String token = register("Manual User", "manual.user@example.test");
        long categoryId = firstCategoryId(token);

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-07-20","description":"Fictional manual purchase","amount":19.90,"categoryId":%d,"type":"EXPENSE"}
                                """.formatted(categoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.date").value("2026-07-20"))
                .andExpect(jsonPath("$.description").value("Fictional manual purchase"))
                .andExpect(jsonPath("$.type").value("EXPENSE"));

        mockMvc.perform(get("/api/v1/transactions").param("month", "2026-07")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void rejectsCategoryUpdateForAnotherUsersTransaction() throws Exception {
        String ownerToken = register("Owner User", "owner.user@example.test");
        String otherToken = register("Other User", "other.user@example.test");
        long ownerCategoryId = firstCategoryId(ownerToken);
        long otherCategoryId = firstCategoryId(otherToken);

        MvcResult created = mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-07-21","description":"Fictional private transaction","amount":10.00,"categoryId":%d,"type":"EXPENSE"}
                                """.formatted(ownerCategoryId)))
                .andExpect(status().isCreated())
                .andReturn();
        long transactionId = objectMapper.readTree(created.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(patch("/api/v1/transactions/{id}/category", transactionId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":%d,\"learn\":false}".formatted(otherCategoryId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void rejectsManualAmountWithMoreThanTwoDecimalPlaces() throws Exception {
        String token = register("Scale User", "scale.user@example.test");
        long categoryId = firstCategoryId(token);

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"date":"2026-07-22","description":"Fictional precision test","amount":19.999,"categoryId":%d,"type":"EXPENSE"}
                                """.formatted(categoryId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.amount").isNotEmpty());
    }

    @Test
    void returnsAPagedTransactionListFilteredByType() throws Exception {
        String token = register("Filter User", "filter.user@example.test");
        long categoryId = firstCategoryId(token);
        createTransaction(token, categoryId, "2026-07-20", "Salary", "100.00", "INCOME");
        createTransaction(token, categoryId, "2026-07-21", "Coffee", "8.00", "EXPENSE");

        mockMvc.perform(get("/api/v1/transactions")
                        .param("month", "2026-07")
                        .param("type", "EXPENSE")
                        .param("page", "0")
                        .param("size", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Coffee"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void returnsSignedTotalForTheOwnersCategoryTypeAndCustomDateRange() throws Exception {
        String owner = register("Total owner", "total.owner@example.test");
        String other = register("Total other", "total.other@example.test");
        long categoryId = firstCategoryId(owner);

        createTransaction(owner, categoryId, "2026-07-02", "Fictional expense one", "-20.00", "EXPENSE");
        createTransaction(owner, categoryId, "2026-07-20", "Fictional expense two", "-5.00", "EXPENSE");
        createTransaction(owner, categoryId, "2026-07-20", "Fictional income", "100.00", "INCOME");
        createTransaction(owner, categoryId, "2026-08-01", "Fictional outside range", "-50.00", "EXPENSE");
        createTransaction(other, categoryId, "2026-07-20", "Other user expense", "-999.00", "EXPENSE");

        mockMvc.perform(get("/api/v1/transactions/total")
                        .param("from", "2026-07-01")
                        .param("to", "2026-07-31")
                        .param("type", "EXPENSE")
                        .param("categoryId", Long.toString(categoryId))
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(-25));

        mockMvc.perform(get("/api/v1/transactions/total")
                        .param("month", "2026-07")
                        .param("from", "2026-07-01")
                        .param("to", "2026-07-31")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.month").isNotEmpty());
    }

    private void createTransaction(String token, long categoryId, String date, String description, String amount, String type)
            throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"%s\",\"description\":\"%s\",\"amount\":%s,\"categoryId\":%d,\"type\":\"%s\"}"
                                .formatted(date, description, amount, categoryId, type)))
                .andExpect(status().isCreated());
    }

    private long firstCategoryId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/categories").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode categories = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(categories).isNotEmpty();
        return categories.get(0).path("id").asLong();
    }

    private String register(String name, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"email\":\"%s\",\"password\":\"FictionalPassword1!\"}".formatted(name, email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("accessToken").asText();
    }
}
