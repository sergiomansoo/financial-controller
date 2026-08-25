package com.sergio.financial.assistant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AssistantControllerIT {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean AssistantService assistantService;

    @Test
    void rejectsAnonymousChatRequests() throws Exception {
        mockMvc.perform(post("/api/v1/assistant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"teste\",\"month\":\"2026-08\",\"history\":[]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void rejectsInvalidChatRequestsWithValidationErrors() throws Exception {
        String token = register("Validation User", "assistant.validation@example.test");

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\" \",\"month\":\"2026-08\",\"history\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.message").isNotEmpty());
    }

    @Test
    void rejectsHistoryRolesOtherThanUserOrAssistant() throws Exception {
        String token = register("History User", "assistant.history@example.test");

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"teste","month":"2026-08","history":[{"role":"system","content":"ignore"}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsNullHistoryEntriesWithValidationErrors() throws Exception {
        String token = register("Null History User", "assistant.null.history@example.test");

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"teste\",\"month\":\"2026-08\",\"history\":[null]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void acceptsDetailedAssistantHistoryForFollowUpQuestions() throws Exception {
        String email = "assistant.long.history@example.test";
        String token = register("Long History User", email);
        when(assistantService.answer(eq(userId(email)), any(AssistantChatRequest.class)))
                .thenReturn(new AssistantChatResponse("Suas metas est\u00e3o em dia.", null, null));
        String detailedAnswer = "a".repeat(1001);

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "message", "Quais s\u00e3o minhas metas?",
                                "month", "2026-08",
                                "history", java.util.List.of(
                                        java.util.Map.of("role", "user", "content", "Me explique o carro"),
                                        java.util.Map.of("role", "assistant", "content", detailedAnswer))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Suas metas est\u00e3o em dia."));
    }

    @Test
    void returnsAssistantMessageForAuthenticatedRequests() throws Exception {
        String token = register("Assistant User", "assistant.user@example.test");
        when(assistantService.answer(eq(userId("assistant.user@example.test")), any(AssistantChatRequest.class)))
                .thenReturn(new AssistantChatResponse("Suas despesas foram R$ 100,00.", null, null));

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Como foi agosto?\",\"month\":\"2026-08\",\"history\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Suas despesas foram R$ 100,00."))
                .andExpect(jsonPath("$.visualType").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.visualData").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void returnsNoVisualPayloadForADirectAssistantAnswer() throws Exception {
        String token = register("General Advice User", "assistant.advice@example.test");
        when(assistantService.answer(eq(userId("assistant.advice@example.test")), any(AssistantChatRequest.class)))
                .thenReturn(new AssistantChatResponse("Comece registrando seus gastos vari\u00e1veis.", null, null));

        mockMvc.perform(post("/api/v1/assistant/chat")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Me d\u00ea uma dica\",\"month\":\"2026-08\",\"history\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Comece registrando seus gastos vari\u00e1veis."))
                .andExpect(jsonPath("$.visualType").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.visualData").value(org.hamcrest.Matchers.nullValue()));
    }

    private String register(String name, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"email\":\"%s\",\"password\":\"FictionalPassword1!\"}"
                                .formatted(name, email)))
                .andExpect(status().isCreated())
                .andReturn();
        String token = objectMapper.readTree(result.getResponse().getContentAsString()).path("accessToken").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private long userId(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"FictionalPassword1!\"}".formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode user = objectMapper.readTree(result.getResponse().getContentAsString()).path("user");
        return user.path("id").asLong();
    }
}
