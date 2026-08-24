package com.sergio.financial.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sergio.financial.auth.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

class SecurityConfigTest {
    @Test
    void disablesContainerRegistrationForTheJwtFilterUsedBySpringSecurity() {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(mock(JwtService.class), new ObjectMapper());

        FilterRegistrationBean<JwtAuthenticationFilter> registration = new SecurityConfig()
                .jwtAuthenticationFilterRegistration(filter);

        assertThat(registration.isEnabled()).isFalse();
        assertThat(registration.getFilter()).isSameAs(filter);
    }
}
