package com.finance.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.finance.exception.ValidationException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class InputParserTest {

    @TempDir
    Path tempDir;

    @Test
    void amountsWithDotOrCommaAreAccepted() {
        assertEquals(new BigDecimal("12.50"), InputParser.parseAmount("12.50"));
        assertEquals(new BigDecimal("12.50"), InputParser.parseAmount(" 12,5 "));
        assertEquals(new BigDecimal("7.00"), InputParser.parseAmount("7"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "abc", "-5", "0", "0,00", "1.234,56", "12.345", "1e3", "€12", "12 50"})
    void invalidAmountsAreRejected(String input) {
        assertThrows(ValidationException.class, () -> InputParser.parseAmount(input));
    }

    @Test
    void validDateIsParsed() {
        assertEquals(LocalDate.of(2026, 10, 27), InputParser.parseDate("2026-10-27"));
        assertEquals(LocalDate.of(2028, 2, 29), InputParser.parseDate("2028-02-29")); // leap year
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "2026-02-30", "2026-13-01", "27.10.2026", "2026/10/27", "2026-1-5", "yesterday"})
    void invalidDatesAreRejected(String input) {
        assertThrows(ValidationException.class, () -> InputParser.parseDate(input));
    }

    @Test
    void yearMonthIsParsed() {
        assertEquals(YearMonth.of(2026, 10), InputParser.parseYearMonth("2026-10"));
        assertThrows(ValidationException.class, () -> InputParser.parseYearMonth("10-2026"));
        assertThrows(ValidationException.class, () -> InputParser.parseYearMonth("2026-13"));
    }

    @Test
    void idsMustBePositiveWholeNumbers() {
        assertEquals(42L, InputParser.parseId(" 42 "));
        assertThrows(ValidationException.class, () -> InputParser.parseId("0"));
        assertThrows(ValidationException.class, () -> InputParser.parseId("-1"));
        assertThrows(ValidationException.class, () -> InputParser.parseId("1.5"));
        assertThrows(ValidationException.class, () -> InputParser.parseId("abc"));
        assertThrows(ValidationException.class, () -> InputParser.parseId("99999999999999999999"));
    }

    @Test
    void menuChoiceMustBeInRange() {
        assertEquals(3, InputParser.parseMenuChoice("3", 0, 11));
        assertThrows(ValidationException.class, () -> InputParser.parseMenuChoice("12", 0, 11));
        assertThrows(ValidationException.class, () -> InputParser.parseMenuChoice("one", 0, 11));
        assertThrows(ValidationException.class, () -> InputParser.parseMenuChoice("", 0, 11));
        assertThrows(ValidationException.class, () -> InputParser.parseMenuChoice("99999999999", 0, 11));
    }

    @Test
    void exportPathMustBeCsvFile() {
        assertEquals(Path.of("out.csv"), InputParser.parseCsvPath("out.csv"));
        assertThrows(ValidationException.class, () -> InputParser.parseCsvPath("out.txt"));
        assertThrows(ValidationException.class, () -> InputParser.parseCsvPath(""));
    }

    @Test
    void exportPathMustNotBeDirectory() {
        Path directory = tempDir.resolve("folder.csv");
        directory.toFile().mkdir();
        assertThrows(ValidationException.class, () -> InputParser.parseCsvPath(directory.toString()));
    }
}
