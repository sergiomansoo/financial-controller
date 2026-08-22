package com.sergio.financial.assistant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sergio.financial.config.GroqProperties;
import com.sergio.financial.error.AiUnavailableException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GroqClient {
    private final GroqProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public GroqClient(GroqProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newBuilder().build());
    }

    GroqClient(GroqProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public ObjectNode complete(List<ObjectNode> messages, ArrayNode tools) {
        validateConfiguration();
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("model", properties.model());
            payload.set("messages", objectMapper.valueToTree(messages));
            payload.set("tools", tools);
            payload.put("tool_choice", "auto");
            payload.put("parallel_tool_calls", false);
            payload.put("temperature", new java.math.BigDecimal("0.2"));
            payload.put("max_completion_tokens", 700);

            HttpRequest request = HttpRequest.newBuilder(endpoint())
                    .timeout(properties.timeout())
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiUnavailableException();
            }
            String body = response.body();
            if (body == null || body.isBlank()) {
                throw new AiUnavailableException();
            }
            JsonNode message = objectMapper.readTree(body).at("/choices/0/message");
            if (!message.isObject()) {
                throw new AiUnavailableException();
            }
            return ((ObjectNode) message).deepCopy();
        } catch (AiUnavailableException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiUnavailableException(exception);
        } catch (JsonProcessingException exception) {
            throw new AiUnavailableException();
        } catch (IOException | IllegalArgumentException exception) {
            throw new AiUnavailableException(exception);
        }
    }

    private void validateConfiguration() {
        if (properties == null || properties.apiKey() == null || properties.apiKey().isBlank()
                || properties.model() == null || properties.model().isBlank()
                || properties.baseUrl() == null || properties.timeout() == null
                || properties.timeout().isZero() || properties.timeout().isNegative()) {
            throw new AiUnavailableException();
        }
    }

    private URI endpoint() {
        String baseUrl = properties.baseUrl().toString();
        URI directory = URI.create(baseUrl.endsWith("/") ? baseUrl : baseUrl + "/");
        return directory.resolve("chat/completions");
    }
}
