package com.finance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.finance.exception.ValidationException;
import com.finance.model.Category;
import com.finance.model.MonthlySummary;
import com.finance.model.Transaction;
import com.finance.model.TransactionType;
import com.finance.repository.InMemoryTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FinanceServiceTest {

    private static final Category SALARY = Category.of("Salary");
    private static final Category FOOD = Category.of("Food");
    private static final Category RENT = Category.of("Rent");

    private FinanceService service;

    @BeforeEach
    void setUp() {
        service = new FinanceService(new InMemoryTransactionRepository());
    }

    private static BigDecimal eur(String value) {
        return new BigDecimal(value);
    }

    private static LocalDate date(int year, int month, int day) {
        return LocalDate.of(year, month, day);
    }

    // ---- Adding ------------------------------------------------------------------------

    @Test
    void addIncomeStoresTransactionWithFirstId() {
        Transaction income = service.addIncome(eur("1200"), SALARY, "October salary", date(2026, 10, 1));

        assertEquals(1, income.id());
        assertEquals(TransactionType.INCOME, income.type());
        assertEquals(eur("1200.00"), income.amount());
        assertEquals(List.of(income), service.getAllTransactions());
    }

    @Test
    void addExpenseAssignsIncreasingIds() {
        Transaction first = service.addExpense(eur("10"), FOOD, "", date(2026, 10, 1));
        Transaction second = service.addExpense(eur("20"), FOOD, "", date(2026, 10, 2));

        assertEquals(1, first.id());
        assertEquals(2, second.id());
        assertEquals(TransactionType.EXPENSE, second.type());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-5", "0.001"})
    void invalidAmountIsRejectedAndNothingIsStored(String amount) {
        assertThrows(ValidationException.class,
                () -> service.addExpense(new BigDecimal(amount), FOOD, "", date(2026, 10, 1)));
        assertTrue(service.getAllTransactions().isEmpty());
    }

    // ---- Totals ------------------------------------------------------------------------

    @Test
    void totalsAreZeroWithoutTransactions() {
        assertEquals(eur("0.00"), service.getTotalIncome());
        assertEquals(eur("0.00"), service.getTotalExpenses());
        assertEquals(eur("0.00"), service.getBalance());
    }

    @Test
    void totalsAndBalanceAreExact() {
        // With double, 0.10 + 0.20 would be 0.30000000000000004.
        service.addIncome(eur("0.10"), SALARY, "", date(2026, 10, 1));
        service.addIncome(eur("0.20"), SALARY, "", date(2026, 10, 2));
        service.addExpense(eur("0.05"), FOOD, "", date(2026, 10, 3));

        assertEquals(eur("0.30"), service.getTotalIncome());
        assertEquals(eur("0.05"), service.getTotalExpenses());
        assertEquals(eur("0.25"), service.getBalance());
    }

    @Test
    void balanceCanBeNegative() {
        service.addIncome(eur("100"), SALARY, "", date(2026, 10, 1));
        service.addExpense(eur("450"), RENT, "", date(2026, 10, 1));

        assertEquals(eur("-350.00"), service.getBalance());
    }

    @Test
    void expensesByCategoryAreSortedLargestFirst() {
        service.addExpense(eur("30"), FOOD, "", date(2026, 10, 1));
        service.addExpense(eur("450"), RENT, "", date(2026, 10, 1));
        service.addExpense(eur("15.50"), FOOD, "", date(2026, 10, 2));
        service.addIncome(eur("1000"), SALARY, "", date(2026, 10, 1));

        Map<Category, BigDecimal> totals = service.getExpensesByCategory();

        assertEquals(List.of(RENT, FOOD), List.copyOf(totals.keySet()));
        assertEquals(eur("45.50"), totals.get(FOOD));
        assertFalse(totals.containsKey(SALARY));
    }

    // ---- Monthly summary ---------------------------------------------------------------

    @Test
    void monthlySummaryOnlyIncludesSelectedMonth() {
        service.addIncome(eur("1200"), SALARY, "", date(2026, 10, 1));
        service.addExpense(eur("450"), RENT, "", date(2026, 10, 3));
        service.addExpense(eur("50"), FOOD, "", date(2026, 10, 31));
        service.addExpense(eur("99"), FOOD, "", date(2026, 9, 30));  // previous month
        service.addExpense(eur("77"), FOOD, "", date(2026, 11, 1));  // next month

        MonthlySummary summary = service.getMonthlySummary(YearMonth.of(2026, 10));

        assertEquals(eur("1200.00"), summary.totalIncome());
        assertEquals(eur("500.00"), summary.totalExpenses());
        assertEquals(eur("700.00"), summary.savings());
        assertEquals(3, summary.transactionCount());
        assertEquals(eur("50.00"), summary.expensesByCategory().get(FOOD));
    }

    @Test
    void monthlySummaryOfEmptyMonthIsZero() {
        MonthlySummary summary = service.getMonthlySummary(YearMonth.of(2026, 1));

        assertEquals(eur("0.00"), summary.savings());
        assertEquals(0, summary.transactionCount());
        assertTrue(summary.expensesByCategory().isEmpty());
    }

    // ---- Filtering ---------------------------------------------------------------------

    @Test
    void findByCategoryIgnoresCase() {
        Transaction food = service.addExpense(eur("12"), FOOD, "", date(2026, 10, 1));
        service.addExpense(eur("450"), RENT, "", date(2026, 10, 1));

        assertEquals(List.of(food), service.findByCategory(Category.of("food")));
        assertTrue(service.findByCategory(Category.of("Healthcare")).isEmpty());
    }

    @Test
    void findByDateRangeIncludesBothEndDates() {
        service.addExpense(eur("1"), FOOD, "before", date(2026, 9, 30));
        Transaction start = service.addExpense(eur("2"), FOOD, "start", date(2026, 10, 1));
        Transaction end = service.addExpense(eur("3"), FOOD, "end", date(2026, 10, 31));
        service.addExpense(eur("4"), FOOD, "after", date(2026, 11, 1));

        assertEquals(List.of(start, end), service.findByDateRange(date(2026, 10, 1), date(2026, 10, 31)));
    }

    @Test
    void findByDateRangeRejectsStartAfterEnd() {
        assertThrows(ValidationException.class,
                () -> service.findByDateRange(date(2026, 10, 31), date(2026, 10, 1)));
    }

    @Test
    void resultsAreSortedByDateThenId() {
        Transaction late = service.addExpense(eur("1"), FOOD, "", date(2026, 10, 20));
        Transaction early = service.addExpense(eur("1"), FOOD, "", date(2026, 10, 5));
        Transaction sameDayLater = service.addExpense(eur("1"), FOOD, "", date(2026, 10, 20));

        assertEquals(List.of(early, late, sameDayLater), service.getAllTransactions());
    }

    @Test
    void searchDescriptionsIgnoresCase() {
        Transaction lunch = service.addExpense(eur("8.90"), FOOD, "Lunch at Mensa", date(2026, 10, 1));
        service.addExpense(eur("450"), RENT, "October rent", date(2026, 10, 1));

        assertEquals(List.of(lunch), service.searchDescriptions("mensa"));
        assertTrue(service.searchDescriptions("cinema").isEmpty());
    }

    @Test
    void searchRejectsBlankText() {
        assertThrows(ValidationException.class, () -> service.searchDescriptions("   "));
    }

    // ---- Deleting ----------------------------------------------------------------------

    @Test
    void deleteRemovesTransactionFromTotals() {
        Transaction expense = service.addExpense(eur("50"), FOOD, "", date(2026, 10, 1));

        assertTrue(service.deleteTransaction(expense.id()));
        assertTrue(service.findById(expense.id()).isEmpty());
        assertEquals(eur("0.00"), service.getTotalExpenses());
    }

    @Test
    void deleteUnknownIdReturnsFalse() {
        assertFalse(service.deleteTransaction(42));
    }
}
