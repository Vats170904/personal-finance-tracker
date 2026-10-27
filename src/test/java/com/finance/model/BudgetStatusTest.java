package com.finance.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class BudgetStatusTest {

    private static BudgetStatus status(String limit, String spent) {
        return new BudgetStatus(YearMonth.of(2026, 10), new BigDecimal(limit), new BigDecimal(spent));
    }

    @Test
    void belowThresholdHasNoWarning() {
        BudgetStatus status = status("500.00", "200.00");
        assertEquals(new BigDecimal("300.00"), status.remaining());
        assertEquals(new BigDecimal("40.0"), status.percentUsed());
        assertFalse(status.isNearLimit());
        assertFalse(status.isLimitReached());
    }

    @Test
    void eightyPercentIsNearLimit() {
        BudgetStatus status = status("500.00", "400.00");
        assertTrue(status.isNearLimit());
        assertFalse(status.isLimitReached());
    }

    @Test
    void exactlyTheLimitCountsAsReachedButNotExceeded() {
        BudgetStatus status = status("500.00", "500.00");
        assertTrue(status.isLimitReached());
        assertFalse(status.isExceeded());
        assertFalse(status.isNearLimit());
        assertEquals(new BigDecimal("0.00"), status.remaining());
    }

    @Test
    void aboveTheLimitIsExceededWithNegativeRemaining() {
        BudgetStatus status = status("500.00", "512.30");
        assertTrue(status.isExceeded());
        assertEquals(new BigDecimal("-12.30"), status.remaining());
        assertEquals(new BigDecimal("102.5"), status.percentUsed());
    }
}
