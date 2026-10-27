package com.finance.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.finance.model.Category;
import com.finance.model.Transaction;
import com.finance.repository.BudgetRepository;
import com.finance.repository.InMemoryTransactionRepository;
import com.finance.service.BudgetService;
import com.finance.service.FinanceService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Drives the real UI with scripted keyboard input, without a real terminal. */
class ConsoleUITest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-27T10:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    private FinanceService finance;
    private BudgetService budget;

    @BeforeEach
    void setUp() {
        finance = new FinanceService(new InMemoryTransactionRepository());
        budget = new BudgetService(new BudgetRepository(tempDir.resolve("budget.properties")), finance);
    }

    /** Runs the UI with the given input lines and returns everything it printed. */
    private String run(String... lines) {
        String script = String.join("\n", lines) + "\n";
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(output, true, StandardCharsets.UTF_8);
        InputReader reader = new InputReader(
                new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8, out);
        new ConsoleUI(finance, budget, reader, out, new MoneyFormatter("€"), FIXED_CLOCK, tempDir).run();
        return output.toString(StandardCharsets.UTF_8);
    }

    @Test
    void addExpenseThroughMenu() {
        String output = run("2", "12,50", "1", "Lunch, with friends", "2026-10-20", "0");

        List<Transaction> all = finance.getAllTransactions();
        assertEquals(1, all.size());
        Transaction saved = all.get(0);
        assertEquals(new BigDecimal("12.50"), saved.amount());
        assertEquals(Category.of("Food"), saved.category());
        assertEquals("Lunch, with friends", saved.description());
        assertEquals(LocalDate.of(2026, 10, 20), saved.date());
        assertTrue(output.contains("Expense recorded: ID 1 | €12.50 | Food | 2026-10-20"), output);
    }

    @Test
    void emptyDateMeansTodayAndCustomCategoryIsAccepted() {
        run("1", "300", "Tutoring", "", "", "0");

        Transaction saved = finance.getAllTransactions().get(0);
        assertEquals(Category.of("Tutoring"), saved.category());
        assertEquals(LocalDate.of(2026, 10, 27), saved.date());
    }

    @Test
    void invalidInputIsReportedAndRetriedWithoutCrashing() {
        String output = run("hello", "42", "2", "-5", "abc", "7.5", "99", "1", "", "2026-02-30", "2026-10-01", "0");

        assertTrue(output.contains("Please enter a number from 0 to 11."), output);
        assertTrue(output.contains("Amount must be greater than zero."), output);
        assertTrue(output.contains("is not a valid amount"), output);
        assertTrue(output.contains("is not a valid date"), output);
        assertEquals(1, finance.getAllTransactions().size());
    }

    @Test
    void cancelReturnsToMenuWithoutSaving() {
        String output = run("2", "10", "cancel", "0");

        assertTrue(output.contains("Cancelled"), output);
        assertTrue(finance.getAllTransactions().isEmpty());
    }

    @Test
    void budgetWarningIsShownWhenExpenseExceedsBudget() {
        String output = run("9", "1", "100", "2", "120", "1", "", "", "0");

        assertTrue(output.contains("WARNING: You have exceeded your 2026-10 budget by €20.00."), output);
    }

    @Test
    void deleteAsksForConfirmation() {
        finance.addExpense(new BigDecimal("5"), Category.of("Food"), "", LocalDate.of(2026, 10, 1));

        run("10", "1", "n", "0");
        assertEquals(1, finance.getAllTransactions().size());

        String output = run("10", "1", "y", "0");
        assertTrue(finance.getAllTransactions().isEmpty());
        assertTrue(output.contains("Transaction 1 deleted."), output);
    }

    @Test
    void exportWritesCsvFile() throws Exception {
        finance.addExpense(new BigDecimal("5"), Category.of("Food"), "Snack", LocalDate.of(2026, 10, 1));
        Path target = tempDir.resolve("export.csv");

        run("11", target.toString(), "0");

        List<String> lines = Files.readAllLines(target, StandardCharsets.UTF_8);
        assertEquals(2, lines.size());
        assertEquals("1,EXPENSE,2026-10-01,5.00,Food,Snack", lines.get(1));
    }

    @Test
    void endOfInputExitsGracefully() {
        String output = run("3");

        assertTrue(output.contains("No transactions found."), output);
        assertTrue(output.contains("Goodbye!"), output);
    }
}
