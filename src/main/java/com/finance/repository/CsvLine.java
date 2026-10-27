package com.finance.repository;

import com.finance.exception.ValidationException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Minimal CSV encoding for a single line, following the common RFC 4180 rules:
 * fields containing a comma, a quote or leading/trailing spaces are wrapped in
 * double quotes, and quotes inside them are doubled ({@code "} becomes {@code ""}).
 *
 * <p>Multi-line fields are not supported; the model rejects line breaks in descriptions.
 */
final class CsvLine {

    private static final char SEPARATOR = ',';
    private static final char QUOTE = '"';

    private CsvLine() {
        // utility class
    }

    static String format(List<String> fields) {
        return fields.stream().map(CsvLine::escape).collect(Collectors.joining(String.valueOf(SEPARATOR)));
    }

    static String escape(String field) {
        boolean needsQuotes = field.indexOf(SEPARATOR) >= 0
                || field.indexOf(QUOTE) >= 0
                || !field.equals(field.strip());
        if (!needsQuotes) {
            return field;
        }
        return QUOTE + field.replace("\"", "\"\"") + QUOTE;
    }

    static List<String> parse(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (inQuotes) {
                boolean escapedQuote = c == QUOTE && i + 1 < line.length() && line.charAt(i + 1) == QUOTE;
                if (escapedQuote) {
                    current.append(QUOTE);
                    i++; // skip the second quote of the pair
                } else if (c == QUOTE) {
                    inQuotes = false;
                } else {
                    current.append(c);
                }
            } else if (c == QUOTE) {
                inQuotes = true;
            } else if (c == SEPARATOR) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
            i++;
        }
        if (inQuotes) {
            throw new ValidationException("unterminated quoted field");
        }
        fields.add(current.toString());
        return fields;
    }
}
