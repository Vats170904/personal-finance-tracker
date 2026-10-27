package com.finance.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.finance.exception.ValidationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TransactionTest {

    private static final Category FOOD = Category.of("Food");
    private static final LocalDate DATE = LocalDate.of(2026, 10, 20);

    private static Transaction expense(String amount, String description) {
        return new Transaction(1, TransactionType.EXPENSE, new BigDecimal(amount), FOOD, description, DATE);
    }

    @Test
    void amountIsStoredWithTwoDecimalPlaces() {
        assertEquals(new BigDecimal("5.00"), expense("5", "").amount());
        assertEquals(new BigDecimal("12.50"), expense("12.5", "").amount());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.00", "-1", "-0.01", "0.001", "12.345", "1000000000.01"})
    void invalidAmountsAreRejected(String amount) {
        assertThrows(ValidationException.class, () -> expense(amount, ""));
    }

    @Test
    void trailingZerosBeyondTwoDecimalsAreAccepted() {
        assertEquals(new BigDecimal("12.50"), expense("12.500", "").amount());
    }

    @Test
    void missingFieldsAreRejected() {
        BigDecimal amount = new BigDecimal("1.00");
        assertThrows(ValidationException.class,
                () -> new Transaction(0, TransactionType.EXPENSE, amount, FOOD, "", DATE));
        assertThrows(ValidationException.class, () -> new Transaction(1, null, amount, FOOD, "", DATE));
        assertThrows(ValidationException.class,
                () -> new Transaction(1, TransactionType.EXPENSE, null, FOOD, "", DATE));
        assertThrows(ValidationException.class,
                () -> new Transaction(1, TransactionType.EXPENSE, amount, null, "", DATE));
        assertThrows(ValidationException.class,
                () -> new Transaction(1, TransactionType.EXPENSE, amount, FOOD, "", null));
    }

    @Test
    void descriptionIsTrimmedAndNullBecomesEmpty() {
        assertEquals("Lunch", expense("1", "  Lunch  ").description());
        assertEquals("", expense("1", null).description());
    }

    @Test
    void descriptionWithLineBreakIsRejected() {
        assertThrows(ValidationException.class, () -> expense("1", "first line\nsecond line"));
    }

    @Test
    void tooLongDescriptionIsRejected() {
        String tooLong = "x".repeat(Transaction.MAX_DESCRIPTION_LENGTH + 1);
        assertThrows(ValidationException.class, () -> expense("1", tooLong));
    }

    @Test
    void yearMonthAndTypeHelpers() {
        Transaction transaction = expense("1", "");
        assertEquals(YearMonth.of(2026, 10), transaction.yearMonth());
        assertTrue(transaction.isExpense());
    }
}
