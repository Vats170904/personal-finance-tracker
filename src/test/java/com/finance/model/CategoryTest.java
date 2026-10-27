package com.finance.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.finance.exception.ValidationException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CategoryTest {

    @Test
    void defaultCategoryNamesUseDefaultSpelling() {
        assertEquals("Food", Category.of("  fOOd ").getName());
    }

    @Test
    void customCategoryKeepsUserSpellingAndCollapsesSpaces() {
        assertEquals("Gym Membership", Category.of("  Gym   Membership ").getName());
    }

    @Test
    void categoriesAreEqualIgnoringCase() {
        assertEquals(Category.of("Gym"), Category.of("GYM"));
        assertEquals(Category.of("Gym").hashCode(), Category.of("gym").hashCode());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "Food,Drinks", "Rent\"", "Café;"})
    void invalidNamesAreRejected(String name) {
        assertThrows(ValidationException.class, () -> Category.of(name));
    }

    @Test
    void tooLongNameIsRejected() {
        assertThrows(ValidationException.class, () -> Category.of("x".repeat(Category.MAX_LENGTH + 1)));
    }

    @Test
    void lettersWithAccentsAreAllowed() {
        assertEquals("Café & Bäckerei", Category.of("Café & Bäckerei").getName());
    }

    @Test
    void defaultsDifferForIncomeAndExpense() {
        List<Category> income = Category.defaultsFor(TransactionType.INCOME);
        List<Category> expense = Category.defaultsFor(TransactionType.EXPENSE);
        assertTrue(income.contains(Category.of("Salary")));
        assertTrue(expense.contains(Category.of("Rent")));
        assertFalse(expense.contains(Category.of("Salary")));
    }
}
