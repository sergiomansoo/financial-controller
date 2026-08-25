package com.sergio.financial.assistant;

import com.fasterxml.jackson.databind.JsonNode;

public record AssistantChatResponse(String message, String visualType, JsonNode visualData) {
}
