package com.sergio.financial.goal;
import jakarta.validation.constraints.*; import java.math.BigDecimal;
public record SavingsGoalMonthRequest(@NotNull @DecimalMin("0.00") @Digits(integer=17,fraction=2) BigDecimal plannedAmount, @NotNull @DecimalMin("0.00") @Digits(integer=17,fraction=2) BigDecimal savedAmount) { }
