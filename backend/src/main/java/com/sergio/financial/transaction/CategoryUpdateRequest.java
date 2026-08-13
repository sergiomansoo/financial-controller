package com.sergio.financial.transaction;

import jakarta.validation.constraints.NotNull;

public record CategoryUpdateRequest(@NotNull Long categoryId, boolean learn) {
}
