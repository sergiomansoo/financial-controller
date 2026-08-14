package com.sergio.financial.category;

import jakarta.validation.constraints.NotNull;

public record CategorySalaryRequest(@NotNull Boolean isSalary) {
}
