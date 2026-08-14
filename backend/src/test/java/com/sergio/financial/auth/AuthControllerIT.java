package com.sergio.financial.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.cors.allowed-origins=http://localhost:5173,http://127.0.0.1:5175")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registersAndUsesTokenToReadSystemCategories() throws Exception {
        String token = register("Ada Lovelace", "ada@example.test", "FictionalPassword1!");

        mockMvc.perform(get("/api/v1/categories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].name").value("Alimenta\u00e7\u00e3o"))
                .andExpect(jsonPath("$[1].name").value("Transporte"))
                .andExpect(jsonPath("$[2].name").value("Mercado/Compras"))
                .andExpect(jsonPath("$[3].name").value("Investimentos"))
                .andExpect(jsonPath("$[4].name").value("Outros"));
    }

    @Test
    void rejectsDuplicateEmailWithConflictError() throws Exception {
        register("Grace Hopper", "grace@example.test", "FictionalPassword1!");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Grace Hopper","email":"grace@example.test","password":"FictionalPassword1!"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void rejectsBadLoginWithUnauthorizedError() throws Exception {
        register("Katherine Johnson", "katherine@example.test", "FictionalPassword1!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"katherine@example.test","password":"WrongFictionalPassword1!"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void logsInRegisteredUser() throws Exception {
        register("Dorothy Vaughan", "dorothy@example.test", "FictionalPassword1!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"dorothy@example.test","password":"FictionalPassword1!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("dorothy@example.test"));
    }

    @Test
    void acceptsFreshLoginTokenForBudgetsAndSavingsGoals() throws Exception {
        register("Login Session", "login.session@example.test", "FictionalPassword1!");
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"login.session@example.test\",\"password\":\"FictionalPassword1!\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(login.getResponse().getContentAsString()).path("accessToken").asText();

        mockMvc.perform(get("/api/v1/budgets").param("month", "2026-07")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/savings-goals").param("month", "2026-07")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsTooLongRegistrationEmailWithFieldErrors() throws Exception {
        String email = "a".repeat(244) + "@example.test";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Mary Jackson","email":"%s","password":"FictionalPassword1!"}
                                """.formatted(email)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.email").isNotEmpty());
    }

    @Test
    void rejectsMalformedJsonWithValidationErrorBody() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Malformed\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Validation failed."))
                .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    @Test
    void rejectsInvalidBearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/categories").header("Authorization", "Bearer not-a-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void rejectsExpiredBearerToken() throws Exception {
        String expiredToken = Jwts.builder()
                .subject("999")
                .issuedAt(Date.from(Instant.now().minusSeconds(120)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get("/api/v1/categories").header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void rejectsAnonymousCategoryRequestsWithUnauthorizedError() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void acceptsLocalFrontendDeleteCorsPreflight() throws Exception {
        mockMvc.perform(options("/api/v1/categories")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "DELETE"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("DELETE")));
    }

    @Test
    void acceptsConfiguredLoopbackFrontendLoginCorsPreflight() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://127.0.0.1:5175")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5175"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("POST")));
    }

    private String register(String name, String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","email":"%s","password":"%s"}
                                """.formatted(name, email, password)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.id").isNumber())
                .andExpect(jsonPath("$.user.name").value(name))
                .andExpect(jsonPath("$.user.email").value(email))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = body.path("accessToken").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private static final String TEST_JWT_SECRET = "test-only-jwt-secret-with-at-least-thirty-two-characters";
}
