package com.sergio.financial.rule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryRuleRequest(@NotBlank @Size(max = 160) String keyword, @NotNull Long categoryId) {
}
