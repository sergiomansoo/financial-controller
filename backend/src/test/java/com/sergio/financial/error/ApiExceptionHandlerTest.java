package com.sergio.financial.error;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

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
}
