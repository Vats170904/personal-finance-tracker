package com.finance.model;

import com.finance.exception.ValidationException;
import java.util.Locale;

/** Whether a transaction adds money (income) or removes it (expense). */
public enum TransactionType {
    INCOME("Income"),
    EXPENSE("Expense");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** Parses "INCOME" or "EXPENSE" (case-insensitive). */
    public static TransactionType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Transaction type is required.");
        }
        try {
            return valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ValidationException(
                    "Unknown transaction type '" + value.strip() + "'. Expected INCOME or EXPENSE.");
        }
    }
}
