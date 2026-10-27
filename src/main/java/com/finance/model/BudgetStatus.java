package com.finance.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;

/**
 * Snapshot of how much of the monthly budget has been used in a given month.
 *
 * @param limit the monthly budget
 * @param spent total expenses in {@code month}
 */
public record BudgetStatus(YearMonth month, BigDecimal limit, BigDecimal spent) {

    /** Percentage at which the user gets an early "almost used up" notice. */
    public static final BigDecimal WARNING_THRESHOLD_PERCENT = new BigDecimal("80");

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    /** Budget left to spend. Negative when the budget is exceeded. */
    public BigDecimal remaining() {
        return limit.subtract(spent);
    }

    /** Spent amount as a percentage of the limit, rounded to one decimal place. */
    public BigDecimal percentUsed() {
        return spent.multiply(ONE_HUNDRED).divide(limit, 1, RoundingMode.HALF_UP);
    }

    /** True when spending has reached or gone beyond the limit. */
    public boolean isLimitReached() {
        return spent.compareTo(limit) >= 0;
    }

    /** True when spending is strictly above the limit. */
    public boolean isExceeded() {
        return spent.compareTo(limit) > 0;
    }

    /** True when at least 80% is used but the limit has not been reached yet. */
    public boolean isNearLimit() {
        return !isLimitReached() && percentUsed().compareTo(WARNING_THRESHOLD_PERCENT) >= 0;
    }
}
