package com.sergio.financial.error;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {
    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void doesNotMislabelNonEmailIntegrityFailuresAsEmailConflicts() {
        var response = handler.dataIntegrity(new DataIntegrityViolationException(
                "unique constraint uq_category_rules_user_description"));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().code()).isEqualTo("DATA_INTEGRITY_VIOLATION");
        assertThat(response.getBody().message()).doesNotContain("email");
    }

    @Test
    void mapsAiUnavailableToTheSafeServiceUnavailableEnvelope() {
        var response = handler.handleAiUnavailable(new AiUnavailableException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isEqualTo(new ErrorResponse(503, "AI_UNAVAILABLE",
                "O assistente de IA está indisponível no momento. Tente novamente em instantes."));
    }
}
