package com.finance.model;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Income, expenses and savings for one calendar month.
 *
 * @param expensesByCategory expense totals per category, largest first
 */
public record MonthlySummary(
        YearMonth month,
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        Map<Category, BigDecimal> expensesByCategory,
        int transactionCount) {

    public MonthlySummary {
        // Defensive, read-only copy that keeps the sort order.
        expensesByCategory = Collections.unmodifiableMap(new LinkedHashMap<>(expensesByCategory));
    }

    /** Income minus expenses. Negative when more was spent than earned. */
    public BigDecimal savings() {
        return totalIncome.subtract(totalExpenses);
    }
}
