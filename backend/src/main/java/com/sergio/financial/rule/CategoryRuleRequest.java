package com.sergio.financial.rule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryRuleRequest(
        @NotBlank(message = "Informe uma palavra-chave.")
        @Size(max = 160, message = "A palavra-chave deve ter no máximo 160 caracteres.")
        String keyword,
        @NotNull(message = "Selecione uma categoria.") Long categoryId) {
}
