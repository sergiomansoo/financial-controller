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

    private String register() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rules manager\",\"email\":\"rules.manager@example.test\",\"password\":\"FictionalPassword1!\"}"))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("accessToken").asText();
    }
}
