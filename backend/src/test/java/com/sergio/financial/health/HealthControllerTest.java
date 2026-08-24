package com.sergio.financial.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class HealthControllerTest {
    @Test
    void returnsAnOkStatusForMonitoring() {
        ResponseEntity<Map<String, String>> response = new HealthController().health();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("status", "ok");
    }
}
