package com.sergio.financial.assistant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssistantHistoryMessage(
        @NotBlank String role,
        @NotBlank @Size(max = 6000) String content) {
    public AssistantHistoryMessage {
        if (!"user".equals(role) && !"assistant".equals(role)) {
            throw new IllegalArgumentException("History role must be user or assistant.");
        }
    }
}
