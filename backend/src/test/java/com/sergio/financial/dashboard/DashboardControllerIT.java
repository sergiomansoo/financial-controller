package com.sergio.financial.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardControllerIT {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void upsertsUserScopedBudgetAndReportsOnlyExpenseSpending() throws Exception {
        String token = register("Budget User", "budget.user@example.test");
        long categoryId = categoryId(token, "Alimentação");
        createTransaction(token, categoryId, "2026-07-10", "Expense", "10.00", "EXPENSE");
        createTransaction(token, categoryId, "2026-07-11", "Income", "100.00", "INCOME");
        createTransaction(token, categoryId, "2026-07-12", "Investment", "50.00", "INVESTMENT");

        mockMvc.perform(put("/api/v1/budgets/{id}", categoryId).param("month", "2026-07")
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limit\":5.00}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.spent").value(10))
                .andExpect(jsonPath("$.exceeded").value(true));

        mockMvc.perform(put("/api/v1/budgets/{id}", categoryId).param("month", "2026-07")
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limit\":10.00}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.limit").value(10))
                .andExpect(jsonPath("$.exceeded").value(false));

        mockMvc.perform(get("/api/v1/budgets").param("month", "2026-07").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].spent").value(10));
    }

    @Test
    void rejectsOtherUsersCategoryAndReturnsDashboardAggregates() throws Exception {
        String owner = register("Owner", "budget.owner@example.test");
        String other = register("Other", "budget.other@example.test");
        long ownerCategory = privateCategoryId("budget.owner@example.test");

        mockMvc.perform(put("/api/v1/budgets/{id}", ownerCategory).param("month", "2026-07")
                        .header("Authorization", "Bearer " + other).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limit\":10.00}"))
                .andExpect(status().isNotFound());

        createTransaction(owner, ownerCategory, "2026-06-20", "June expense", "3.00", "EXPENSE");
        createTransaction(owner, ownerCategory, "2026-07-20", "July expense", "7.00", "EXPENSE");
        mockMvc.perform(get("/api/v1/dashboard").param("month", "2026-07").header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.byCategory[0].spent").value(7))
                .andExpect(jsonPath("$.monthlyEvolution.length()").value(6))
                .andExpect(jsonPath("$.monthlyEvolution[0].month").value("2026-02"))
                .andExpect(jsonPath("$.monthlyEvolution[0].income").value(0))
                .andExpect(jsonPath("$.monthlyEvolution[0].expense").value(0))
                .andExpect(jsonPath("$.monthlyEvolution[4].expense").value(3))
                .andExpect(jsonPath("$.monthlyEvolution[5].expense").value(7))
                .andExpect(jsonPath("$.budgets").isArray());
    }

    @Test
    void returnsStandardValidationErrorsForMissingOrInvalidMonth() throws Exception {
        String token = register("Month User", "month.user@example.test");

        mockMvc.perform(get("/api/v1/budgets").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.month").exists());

        mockMvc.perform(get("/api/v1/dashboard").param("month", "July-2026")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors.month").exists());
    }

    private void createTransaction(String token, long categoryId, String date, String description, String amount, String type) throws Exception {
        mockMvc.perform(post("/api/v1/transactions").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"%s\",\"description\":\"%s\",\"amount\":%s,\"categoryId\":%d,\"type\":\"%s\"}"
                                .formatted(date, description, amount, categoryId, type)))
                .andExpect(status().isCreated());
    }

    private long categoryId(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/categories").header("Authorization", "Bearer " + token)).andReturn();
        for (JsonNode category : objectMapper.readTree(result.getResponse().getContentAsString())) {
            if (name.equals(category.path("name").asText())) return category.path("id").asLong();
        }
        throw new AssertionError("Missing category " + name);
    }

    private long privateCategoryId(String email) {
        Long userId = jdbcTemplate.queryForObject("select id from app_users where email = ?", Long.class, email);
        jdbcTemplate.update("insert into categories (name, system_category, user_id) values (?, false, ?)",
                "Private travel", userId);
        return jdbcTemplate.queryForObject("select id from categories where name = ? and user_id = ?",
                Long.class, "Private travel", userId);
    }

    private String register(String name, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"%s\",\"email\":\"%s\",\"password\":\"FictionalPassword1!\"}".formatted(name, email)))
                .andExpect(status().isCreated()).andReturn();
        String token = objectMapper.readTree(result.getResponse().getContentAsString()).path("accessToken").asText();
        assertThat(token).isNotBlank();
        return token;
    }
}
