package com.sergio.financial.goal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sergio.financial.user.UserRepository;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SavingsGoalControllerIT {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired SavingsGoalRepository goals;
    @Autowired SavingsGoalMonthRepository goalMonths;
    @Autowired UserRepository users;

    @Test
    void managesMonthlySavingsGoalsOnlyForTheirOwner() throws Exception {
        String owner = register("Goal owner", "goal.owner@example.test");
        String other = register("Goal other", "goal.other@example.test");
        MvcResult created = mockMvc.perform(post("/api/v1/savings-goals").header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Emergency reserve\",\"targetAmount\":1000.00,\"targetDate\":\"2027-12-31\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Emergency reserve"))
                .andReturn();
        long goalId = objectMapper.readTree(created.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(put("/api/v1/savings-goals/{id}/months/{month}", goalId, "2026-07")
                        .header("Authorization", "Bearer " + owner).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plannedAmount\":200.00,\"savedAmount\":50.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plannedAmount").value(200))
                .andExpect(jsonPath("$.savedAmount").value(50))
                .andExpect(jsonPath("$.progressPercent").value(25));

        mockMvc.perform(get("/api/v1/savings-goals").param("month", "2026-07")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].overallSavedAmount").value(50));

        mockMvc.perform(put("/api/v1/savings-goals/{id}/months/{month}", goalId, "2026-07")
                        .header("Authorization", "Bearer " + other).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plannedAmount\":1.00,\"savedAmount\":1.00}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/savings-goals/{id}", goalId)
                        .header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());

        assertThat(goals.findById(goalId)).isPresent();
        assertThat(goalMonths.countByUserIdAndGoalId(userId("goal.owner@example.test"), goalId)).isEqualTo(1);

        mockMvc.perform(delete("/api/v1/savings-goals/{id}", goalId)
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isNoContent());

        assertThat(goals.findById(goalId)).isEmpty();
        assertThat(goalMonths.countByUserIdAndGoalId(userId("goal.owner@example.test"), goalId)).isZero();
    }

    private String register(String name, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"email\":\"%s\",\"password\":\"FictionalPassword1!\"}".formatted(name, email)))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("accessToken").asText();
    }

    private long userId(String email) {
        return users.findByEmail(email).orElseThrow().getId();
    }
}
