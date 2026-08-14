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
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].spent").value(10))
                .andExpect(jsonPath("$[0].limit").value(10))
                .andExpect(jsonPath("$[1].spent").value(0))
                .andExpect(jsonPath("$[1].limit").value(0));
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

    @Test
    void appliesBothFilterToDashboardAggregates() throws Exception {
        String token = register("Filter dashboard", "filter.dashboard@example.test");
        long categoryId = categoryId(token, "Alimenta\u00e7\u00e3o");
        createTransaction(token, categoryId, "2026-07-20", "Salary", "100.00", "INCOME");
        createTransaction(token, categoryId, "2026-07-21", "Lunch", "30.00", "EXPENSE");
        createTransaction(token, categoryId, "2026-07-22", "Investment", "50.00", "INVESTMENT");

        mockMvc.perform(get("/api/v1/dashboard").param("month", "2026-07").param("filter", "both")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byCategory[0].spent").value(180))
                .andExpect(jsonPath("$.totals.income").value(100))
                .andExpect(jsonPath("$.totals.expense").value(30))
                .andExpect(jsonPath("$.totals.balance").value(70))
                .andExpect(jsonPath("$.monthlyEvolution[5].income").value(100))
                .andExpect(jsonPath("$.monthlyEvolution[5].expense").value(30));
    }

    @Test
    void appliesIncomeFilterToDashboardAggregates() throws Exception {
        String token = register("Income filter", "income.filter@example.test");
        long categoryId = categoryId(token, "Alimenta\u00e7\u00e3o");
        createTransaction(token, categoryId, "2026-07-20", "Salary", "100.00", "INCOME");
        createTransaction(token, categoryId, "2026-07-21", "Lunch", "30.00", "EXPENSE");

        mockMvc.perform(get("/api/v1/dashboard").param("month", "2026-07").param("filter", "income")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byCategory[0].spent").value(100))
                .andExpect(jsonPath("$.totals.income").value(100))
                .andExpect(jsonPath("$.totals.expense").value(0))
                .andExpect(jsonPath("$.totals.balance").value(100))
                .andExpect(jsonPath("$.monthlyEvolution[5].income").value(100))
                .andExpect(jsonPath("$.monthlyEvolution[5].expense").value(0));
    }

    @Test
    void appliesExpenseFilterToDashboardAggregates() throws Exception {
        String token = register("Expense filter", "expense.filter@example.test");
        long categoryId = categoryId(token, "Alimenta\u00e7\u00e3o");
        createTransaction(token, categoryId, "2026-07-20", "Salary", "100.00", "INCOME");
        createTransaction(token, categoryId, "2026-07-21", "Lunch", "30.00", "EXPENSE");

        mockMvc.perform(get("/api/v1/dashboard").param("month", "2026-07").param("filter", "expense")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byCategory[0].spent").value(30))
                .andExpect(jsonPath("$.totals.income").value(0))
                .andExpect(jsonPath("$.totals.expense").value(30))
                .andExpect(jsonPath("$.totals.balance").value(-30))
                .andExpect(jsonPath("$.monthlyEvolution[5].income").value(0))
                .andExpect(jsonPath("$.monthlyEvolution[5].expense").value(30));
    }

    @Test
    void usesAbsoluteImportedDebitsForLargestExpenseAndBudgets() throws Exception {
        String token = register("Imported debits", "imported.debits@example.test");
        long foodCategoryId = categoryId(token, "Alimenta\u00e7\u00e3o");
        long outrosCategoryId = categoryId(token, "Outros");

        createTransaction(token, foodCategoryId, "2026-07-15", "Bakery debit", "-10.00", "EXPENSE");
        createTransaction(token, outrosCategoryId, "2026-07-16", "Fictional service debit", "-100.00", "EXPENSE");

        mockMvc.perform(get("/api/v1/dashboard").param("month", "2026-07").param("filter", "expense")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.expense").value(110))
                .andExpect(jsonPath("$.totals.largestExpenseCategory").value("Outros"))
                .andExpect(jsonPath("$.totals.largestExpenseAmount").value(100));

        mockMvc.perform(put("/api/v1/budgets/{id}", outrosCategoryId).param("month", "2026-07")
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"limit\":200.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spent").value(100))
                .andExpect(jsonPath("$.exceeded").value(false));
    }

    @Test
    void returnsIndependentStructuredHighlightsForEachDashboardFilter() throws Exception {
        String owner = register("Highlight owner", "highlights.owner@example.test");
        String other = register("Highlight other", "highlights.other@example.test");
        long foodCategory = categoryId(owner, "Alimenta\u00e7\u00e3o");
        long otherCategory = categoryId(owner, "Outros");
        long investmentCategory = categoryId(owner, "Investimentos");

        createTransaction(owner, foodCategory, "2026-07-10", "Fictional small income", "100.00", "INCOME");
        createTransaction(owner, otherCategory, "2026-07-11", "Fictional largest income", "300.00", "INCOME");
        createTransaction(owner, foodCategory, "2026-07-12", "Fictional small expense", "-25.00", "EXPENSE");
        createTransaction(owner, otherCategory, "2026-07-13", "Fictional largest expense", "-100.00", "EXPENSE");
        createTransaction(owner, investmentCategory, "2026-07-14", "Fictional investment", "1000.00", "INVESTMENT");
        createTransaction(other, foodCategory, "2026-07-15", "Other user income", "999.00", "INCOME");
        createTransaction(other, foodCategory, "2026-07-16", "Other user expense", "-999.00", "EXPENSE");

        mockMvc.perform(get("/api/v1/dashboard").param("month", "2026-07").param("filter", "income")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.largestExpense").value(nullValue()))
                .andExpect(jsonPath("$.totals.largestIncome.categoryName").value("Outros"))
                .andExpect(jsonPath("$.totals.largestIncome.amount").value(300));

        mockMvc.perform(get("/api/v1/dashboard").param("month", "2026-07").param("filter", "expense")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.largestExpense.categoryName").value("Outros"))
                .andExpect(jsonPath("$.totals.largestExpense.amount").value(100))
                .andExpect(jsonPath("$.totals.largestIncome").value(nullValue()))
                .andExpect(jsonPath("$.totals.largestExpenseCategory").value("Outros"))
                .andExpect(jsonPath("$.totals.largestExpenseAmount").value(100));

        mockMvc.perform(get("/api/v1/dashboard").param("month", "2026-07").param("filter", "both")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.largestExpense.categoryName").value("Outros"))
                .andExpect(jsonPath("$.totals.largestIncome.categoryName").value("Outros"));
    }

    @Test
    void reportsUserScopedCommitmentAndInvestmentPercentages() throws Exception {
        String owner = register("Metrics owner", "metrics.owner@example.test");
        String other = register("Metrics other", "metrics.other@example.test");
        long salaryCategory = createCategory(owner, "Fictional salary", true);
        long nonSalaryIncomeCategory = createCategory(owner, "Fictional other income", false);
        long expenseCategory = categoryId(owner, "Outros");
        long investmentCategory = categoryId(owner, "Investimentos");

        createTransaction(owner, salaryCategory, "2026-07-10", "Fictional salary", "300.00", "INCOME");
        createTransaction(owner, expenseCategory, "2026-07-11", "Fictional expense", "-100.00", "EXPENSE");
        createTransaction(owner, nonSalaryIncomeCategory, "2026-07-12", "Fictional income", "100.00", "INCOME");
        createTransaction(owner, investmentCategory, "2026-07-13", "Fictional investment", "50.00", "INVESTMENT");
        createTransaction(other, expenseCategory, "2026-07-14", "Other user income", "900.00", "INCOME");

        mockMvc.perform(patch("/api/v1/categories/{id}", salaryCategory)
                        .header("Authorization", "Bearer " + other).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isSalary\":false}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));

        mockMvc.perform(get("/api/v1/dashboard").param("month", "2026-07").param("filter", "both")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.salaryCommittedPercent").value(33.33))
                .andExpect(jsonPath("$.totals.receivedInvestedPercent").value(16.67));

        createTransaction(other, expenseCategory, "2026-08-10", "Other expense", "-10.00", "EXPENSE");

        mockMvc.perform(get("/api/v1/dashboard").param("month", "2026-08")
                        .header("Authorization", "Bearer " + other))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals.salaryCommittedPercent").value(0))
                .andExpect(jsonPath("$.totals.receivedInvestedPercent").value(0));
    }

    private long createCategory(String token, String name, boolean isSalary) throws Exception {
        MvcResult category = mockMvc.perform(post("/api/v1/categories").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"isSalary\":%s}".formatted(name, isSalary)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSalary").value(isSalary))
                .andReturn();
        return objectMapper.readTree(category.getResponse().getContentAsString()).path("id").asLong();
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
