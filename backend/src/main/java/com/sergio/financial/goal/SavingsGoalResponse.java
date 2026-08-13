package com.sergio.financial.goal;
import java.math.BigDecimal; import java.time.LocalDate;
public record SavingsGoalResponse(Long id,String name,BigDecimal targetAmount,LocalDate targetDate,String month,BigDecimal plannedAmount,BigDecimal savedAmount,BigDecimal progressPercent,BigDecimal overallSavedAmount,BigDecimal overallProgressPercent) { }
