package com.finance.ui;

import com.finance.exception.ValidationException;
import com.finance.model.Money;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Converts raw text typed by the user into typed values.
 *
 * <p>Every method either returns a valid value or throws a {@link ValidationException}
 * with a message that can be shown to the user. The methods are pure (no I/O with the
 * terminal), so they are easy to unit test.
 */
public final class InputParser {

    private static final Pattern DECIMAL_NUMBER = Pattern.compile("\\d+(\\.\\d+)?");
    private static final Pattern WHOLE_NUMBER = Pattern.compile("\\d+");

    private InputParser() {
        // utility class
    }

    /** Accepts "12.50" and the German style "12,50". Thousands separators are not allowed. */
    public static BigDecimal parseAmount(String input) {
        String text = input == null ? "" : input.strip();
        if (text.isEmpty()) {
            throw new ValidationException("Please enter an amount.");
        }
        if (text.startsWith("-")) {
            throw new ValidationException("Amount must be greater than zero.");
        }
        if (text.contains(",") && text.contains(".")) {
            throw new ValidationException(
                    "Use either a dot or a comma as decimal separator, without thousands separators (e.g. 1250.50).");
        }
        String normalized = text.replace(',', '.');
        if (!DECIMAL_NUMBER.matcher(normalized).matches()) {
            throw new ValidationException("'" + text + "' is not a valid amount. Example: 12.50");
        }
        return Money.requirePositive(new BigDecimal(normalized), "Amount");
    }

    /** Parses an ISO 8601 date such as 2026-10-27. Impossible dates like 2026-02-30 are rejected. */
    public static LocalDate parseDate(String input) {
        String text = input == null ? "" : input.strip();
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new ValidationException(
                    "'" + text + "' is not a valid date. Use the format yyyy-MM-dd, e.g. 2026-10-27.");
        }
    }

    /** Parses a month such as 2026-10. */
    public static YearMonth parseYearMonth(String input) {
        String text = input == null ? "" : input.strip();
        try {
            return YearMonth.parse(text);
        } catch (DateTimeParseException e) {
            throw new ValidationException(
                    "'" + text + "' is not a valid month. Use the format yyyy-MM, e.g. 2026-10.");
        }
    }

    public static long parseId(String input) {
        String text = input == null ? "" : input.strip();
        if (!WHOLE_NUMBER.matcher(text).matches()) {
            throw new ValidationException("'" + text + "' is not a valid ID. Please enter a positive whole number.");
        }
        try {
            long id = Long.parseLong(text);
            if (id <= 0) {
                throw new ValidationException("ID must be a positive number.");
            }
            return id;
        } catch (NumberFormatException e) {
            throw new ValidationException("'" + text + "' is too large to be a valid ID.");
        }
    }

    /** Parses a number between {@code min} and {@code max} (inclusive). */
    public static int parseMenuChoice(String input, int min, int max) {
        String text = input == null ? "" : input.strip();
        String rangeMessage = "Please enter a number from " + min + " to " + max + ".";
        if (!WHOLE_NUMBER.matcher(text).matches()) {
            throw new ValidationException(rangeMessage);
        }
        try {
            int choice = Integer.parseInt(text);
            if (choice < min || choice > max) {
                throw new ValidationException(rangeMessage);
            }
            return choice;
        } catch (NumberFormatException e) {
            throw new ValidationException(rangeMessage);
        }
    }

    /** Parses a file path for CSV export. The file name must end with ".csv". */
    public static Path parseCsvPath(String input) {
        String text = input == null ? "" : input.strip();
        if (text.isEmpty()) {
            throw new ValidationException("Please enter a file path.");
        }
        Path path;
        try {
            path = Path.of(text);
        } catch (InvalidPathException e) {
            throw new ValidationException("'" + text + "' is not a valid file path.");
        }
        if (!text.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new ValidationException("The file name must end with .csv");
        }
        if (Files.isDirectory(path)) {
            throw new ValidationException("'" + text + "' is a directory. Please enter a file name.");
        }
        return path;
    }
}
