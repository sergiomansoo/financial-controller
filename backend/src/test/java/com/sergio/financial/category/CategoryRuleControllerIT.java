package com.sergio.financial.category;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoryRuleControllerIT {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Test
    void createsUserCategoryAndKeywordRule() throws Exception {
        String token = register();
        MvcResult category = mockMvc.perform(post("/api/v1/categories").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Viagens\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("Viagens")).andReturn();
        long categoryId = objectMapper.readTree(category.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(post("/api/v1/category-rules").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"hotel central\",\"categoryId\":%d}".formatted(categoryId)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.keyword").value("hotel central"))
                .andExpect(jsonPath("$.category.name").value("Viagens"));

        mockMvc.perform(get("/api/v1/category-rules").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].keyword").value("hotel central"));
    }

    @Test
    void acceptsPortugueseKeywordForAccessibleCategoryAndExplainsInvalidInput() throws Exception {
        String token = register("Portuguese rules", "rules.portuguese@example.test");
        long accessibleCategoryId = firstCategoryId(token);

        mockMvc.perform(post("/api/v1/category-rules").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"  Café da Manhã  \",\"categoryId\":%d}".formatted(accessibleCategoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.keyword").value("cafe da manha"));

        mockMvc.perform(post("/api/v1/category-rules").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"   \",\"categoryId\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Validation failed."))
                .andExpect(jsonPath("$.fieldErrors.keyword").value("Informe uma palavra-chave."))
                .andExpect(jsonPath("$.fieldErrors.categoryId").value("Selecione uma categoria."));
    }

    @Test
    void appliesRuleToNormalizedDescriptionPrefixAndReturnsOnlyChangedTransactions() throws Exception {
        String owner = register("Rule apply owner", "rule.apply.owner@example.test");
        long otherCategoryId = createCategory(owner, "Before reclassification");
        long targetCategoryId = createCategory(owner, "After reclassification");
        long ruleId = createRule(owner, targetCategoryId, "Cafe");
        createTransaction(owner, otherCategoryId, "CAF\u00c9 da manh\u00e3", "2026-08-10");
        createTransaction(owner, otherCategoryId, "Meu caf\u00e9", "2026-08-11");
        createTransaction(owner, targetCategoryId, "Caf\u00e9 j\u00e1 correto", "2026-08-12");

        mockMvc.perform(post("/api/v1/category-rules/{ruleId}/apply", ruleId)
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changedCount").value(1));

        mockMvc.perform(get("/api/v1/transactions").param("month", "2026-08")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.description == 'CAF\u00c9 da manh\u00e3')].category.name")
                        .value("After reclassification"))
                .andExpect(jsonPath("$[?(@.description == 'Meu caf\u00e9')].category.name")
                        .value("Before reclassification"));

        mockMvc.perform(post("/api/v1/category-rules/{ruleId}/apply", ruleId)
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changedCount").value(0));
    }

    @Test
    void rejectsApplyingAnotherUsersRuleWithoutChangingTheirTransactions() throws Exception {
        String owner = register("Rule owner", "rule.owner@example.test");
        String other = register("Rule other", "rule.other@example.test");
        long ownerCategoryId = createCategory(owner, "Owner category");
        long ruleId = createRule(owner, ownerCategoryId, "Mercado");

        mockMvc.perform(post("/api/v1/category-rules/{ruleId}/apply", ruleId)
                        .header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_RULE_NOT_FOUND"));
    }

    @Test
    void deletesOnlyTheOwnersUnusedCategoriesAndRules() throws Exception {
        String owner = register("Owner", "category.owner@example.test");
        String other = register("Other", "category.other@example.test");
        long removableCategory = createCategory(owner, "Removable");
        long foreignCategory = createCategory(owner, "Private owner category");
        long ruleCategory = createCategory(owner, "Rule target");
        long ruleId = createRule(owner, ruleCategory, "fictional merchant");

        mockMvc.perform(delete("/api/v1/categories/{id}", foreignCategory)
                        .header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"));

        mockMvc.perform(delete("/api/v1/categories/{id}", removableCategory)
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/category-rules/{id}", ruleId)
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/categories").header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.name == 'Removable')]").isEmpty());
    }

    @Test
    void rejectsSystemAndReferencedCategoryDeletionWithStandardErrors() throws Exception {
        String token = register("Deletion user", "category.delete@example.test");
        long systemCategory = firstCategoryId(token);
        long referencedCategory = createCategory(token, "Referenced");
        createRule(token, referencedCategory, "fictional reference");

        mockMvc.perform(delete("/api/v1/categories/{id}", systemCategory)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_DELETABLE"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        mockMvc.perform(delete("/api/v1/categories/{id}", referencedCategory)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("CATEGORY_IN_USE"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    private long createCategory(String token, String name) throws Exception {
        MvcResult category = mockMvc.perform(post("/api/v1/categories").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"%s\"}".formatted(name)))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(category.getResponse().getContentAsString()).path("id").asLong();
    }

    private long createRule(String token, long categoryId, String keyword) throws Exception {
        MvcResult rule = mockMvc.perform(post("/api/v1/category-rules").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"keyword\":\"%s\",\"categoryId\":%d}".formatted(keyword, categoryId)))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(rule.getResponse().getContentAsString()).path("id").asLong();
    }

    private void createTransaction(String token, long categoryId, String description, String date) throws Exception {
        mockMvc.perform(post("/api/v1/transactions").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"%s\",\"description\":\"%s\",\"amount\":\"-10.00\",\"categoryId\":%d,\"type\":\"EXPENSE\"}"
                                .formatted(date, description, categoryId)))
                .andExpect(status().isCreated());
    }

    private long firstCategoryId(String token) throws Exception {
        MvcResult categories = mockMvc.perform(get("/api/v1/categories").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(categories.getResponse().getContentAsString()).get(0).path("id").asLong();
    }

    private String register() throws Exception {
        return register("Rules manager", "rules.manager@example.test");
    }

    private String register(String name, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"email\":\"%s\",\"password\":\"FictionalPassword1!\"}".formatted(name, email)))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("accessToken").asText();
    }
}
