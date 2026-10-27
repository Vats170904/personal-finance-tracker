package com.finance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.finance.exception.DataAccessException;
import com.finance.exception.ValidationException;
import com.finance.model.BudgetStatus;
import com.finance.model.Category;
import com.finance.repository.BudgetRepository;
import com.finance.repository.InMemoryTransactionRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BudgetServiceTest {

    private static final YearMonth OCTOBER = YearMonth.of(2026, 10);
    private static final Category FOOD = Category.of("Food");

    @TempDir
    Path tempDir;

    private Path budgetFile;
    private FinanceService finance;
    private BudgetService budget;

    @BeforeEach
    void setUp() {
        budgetFile = tempDir.resolve("budget.properties");
        finance = new FinanceService(new InMemoryTransactionRepository());
        budget = new BudgetService(new BudgetRepository(budgetFile), finance);
    }

    private void spend(String amount, LocalDate date) {
        finance.addExpense(new BigDecimal(amount), FOOD, "", date);
    }

    @Test
    void noBudgetByDefault() {
        assertTrue(budget.getMonthlyBudget().isEmpty());
        assertTrue(budget.getStatus(OCTOBER).isEmpty());
    }

    @Test
    void statusCountsOnlyExpensesOfThatMonth() {
        budget.setMonthlyBudget(new BigDecimal("500"));
        spend("120.50", LocalDate.of(2026, 10, 2));
        spend("79.50", LocalDate.of(2026, 10, 30));
        spend("300", LocalDate.of(2026, 11, 1)); // different month
        finance.addIncome(new BigDecimal("1000"), Category.of("Salary"), "", LocalDate.of(2026, 10, 1));

        BudgetStatus status = budget.getStatus(OCTOBER).orElseThrow();

        assertEquals(new BigDecimal("200.00"), status.spent());
        assertEquals(new BigDecimal("300.00"), status.remaining());
        assertFalse(status.isLimitReached());
    }

    @Test
    void warningWhenBudgetReachedAndExceeded() {
        budget.setMonthlyBudget(new BigDecimal("100"));
        spend("100", LocalDate.of(2026, 10, 1));
        assertTrue(budget.getStatus(OCTOBER).orElseThrow().isLimitReached());
        assertFalse(budget.getStatus(OCTOBER).orElseThrow().isExceeded());

        spend("0.01", LocalDate.of(2026, 10, 2));
        BudgetStatus status = budget.getStatus(OCTOBER).orElseThrow();
        assertTrue(status.isExceeded());
        assertEquals(new BigDecimal("-0.01"), status.remaining());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-100", "10.555"})
    void invalidBudgetIsRejected(String limit) {
        assertThrows(ValidationException.class, () -> budget.setMonthlyBudget(new BigDecimal(limit)));
        assertTrue(budget.getMonthlyBudget().isEmpty());
        assertFalse(Files.exists(budgetFile));
    }

    @Test
    void budgetPersistsAcrossRestarts() {
        budget.setMonthlyBudget(new BigDecimal("450.5"));

        BudgetService restarted = new BudgetService(new BudgetRepository(budgetFile), finance);

        assertEquals(new BigDecimal("450.50"), restarted.getMonthlyBudget().orElseThrow());
    }

    @Test
    void clearedBudgetStaysClearedAfterRestart() {
        budget.setMonthlyBudget(new BigDecimal("450"));
        budget.clearMonthlyBudget();

        BudgetService restarted = new BudgetService(new BudgetRepository(budgetFile), finance);

        assertTrue(restarted.getMonthlyBudget().isEmpty());
    }

    @Test
    void corruptedBudgetFileIsReported() throws IOException {
        Files.writeString(budgetFile, "monthly.limit=abc\n");

        assertThrows(DataAccessException.class,
                () -> new BudgetService(new BudgetRepository(budgetFile), finance));
    }
}
