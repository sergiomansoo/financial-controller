package com.sergio.financial.dashboard;

import com.sergio.financial.transaction.TransactionType;
import java.util.Locale;

public enum DashboardFilter {
    BOTH, INCOME, EXPENSE;

    public static DashboardFilter from(String value) {
        return value == null ? BOTH : DashboardFilter.valueOf(value.toUpperCase(Locale.ROOT));
    }

    public TransactionType transactionType() {
        return switch (this) {
            case INCOME -> TransactionType.INCOME;
            case EXPENSE -> TransactionType.EXPENSE;
            case BOTH -> null;
        };
    }
}
