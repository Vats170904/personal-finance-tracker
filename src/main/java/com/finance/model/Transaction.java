package com.finance.model;

import com.finance.exception.ValidationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * A single income or expense entry.
 *
 * <p>This is a record, so it is immutable: all fields are validated once in the
 * constructor and a transaction can never be put into an invalid state afterwards.
 *
 * @param id          unique, positive identifier
 * @param type        income or expense
 * @param amount      positive amount with two decimal places (the sign comes from {@code type})
 * @param category    validated category
 * @param description optional free text, may be empty but never null
 * @param date        date the money was received or spent
 */
public record Transaction(
        long id,
        TransactionType type,
        BigDecimal amount,
        Category category,
        String description,
        LocalDate date) {

    public static final int MAX_DESCRIPTION_LENGTH = 100;

    /** Compact constructor: validates and normalizes the fields before they are assigned. */
    public Transaction {
        if (id <= 0) {
            throw new ValidationException("Transaction ID must be a positive number.");
        }
        if (type == null) {
            throw new ValidationException("Transaction type is required.");
        }
        if (category == null) {
            throw new ValidationException("Category is required.");
        }
        if (date == null) {
            throw new ValidationException("Date is required.");
        }
        amount = Money.requirePositive(amount, "Amount");
        description = normalizeDescription(description);
    }

    /**
     * Trims a description and checks its length. Line breaks are rejected because
     * every transaction must fit on one line of the CSV file.
     */
    public static String normalizeDescription(String description) {
        if (description == null) {
            return "";
        }
        String trimmed = description.strip();
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ValidationException(
                    "Description must be at most " + MAX_DESCRIPTION_LENGTH + " characters long.");
        }
        if (trimmed.chars().anyMatch(Character::isISOControl)) {
            throw new ValidationException("Description must not contain line breaks or control characters.");
        }
        return trimmed;
    }

    public boolean isIncome() {
        return type == TransactionType.INCOME;
    }

    public boolean isExpense() {
        return type == TransactionType.EXPENSE;
    }

    public YearMonth yearMonth() {
        return YearMonth.from(date);
    }
}
