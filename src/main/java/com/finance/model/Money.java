package com.finance.model;

import com.finance.exception.ValidationException;
import java.math.BigDecimal;

/**
 * Shared rules for monetary values. All amounts in the application are stored
 * as {@link BigDecimal} with exactly two decimal places.
 */
public final class Money {

    public static final int SCALE = 2;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE);
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000.00");

    private Money() {
        // utility class
    }

    /**
     * Validates that {@code amount} is a positive value with at most two decimal places
     * and returns it with a scale of exactly two (e.g. 5 becomes 5.00).
     *
     * @param label name of the value, used in error messages (e.g. "Amount")
     */
    public static BigDecimal requirePositive(BigDecimal amount, String label) {
        if (amount == null) {
            throw new ValidationException(label + " is required.");
        }
        if (amount.signum() <= 0) {
            throw new ValidationException(label + " must be greater than zero.");
        }
        // stripTrailingZeros() turns 12.500 into 12.5, so only real extra precision is rejected.
        if (amount.stripTrailingZeros().scale() > SCALE) {
            throw new ValidationException(label + " may have at most " + SCALE + " decimal places.");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new ValidationException(label + " must not exceed " + MAX_AMOUNT.toPlainString() + ".");
        }
        return amount.setScale(SCALE);
    }
}
