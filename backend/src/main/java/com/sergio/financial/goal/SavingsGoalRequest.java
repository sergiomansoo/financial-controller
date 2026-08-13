package com.sergio.financial.goal;
import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.LocalDate;
public record SavingsGoalRequest(@NotBlank @Size(max=120) String name, @NotNull @DecimalMin("0.00") @Digits(integer=17,fraction=2) BigDecimal targetAmount, LocalDate targetDate) { }
