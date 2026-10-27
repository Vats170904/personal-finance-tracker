package com.finance.model;

import com.finance.exception.ValidationException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A validated category name such as "Food" or a custom one such as "Gym".
 *
 * <p>Categories are compared case-insensitively, so "food" and "Food" are the same
 * category. Names that match a default category are stored with the default spelling.
 */
public final class Category implements Comparable<Category> {

    public static final int MAX_LENGTH = 30;

    private static final Pattern ALLOWED_CHARACTERS = Pattern.compile("[\\p{L}\\p{N} &'\\-]+");

    private static final List<String> INCOME_DEFAULTS = List.of("Salary", "Scholarship", "Other");
    private static final List<String> EXPENSE_DEFAULTS = List.of(
            "Food", "Rent", "Transportation", "Education",
            "Shopping", "Entertainment", "Healthcare", "Other");

    private final String name;
    private final String key;

    private Category(String name) {
        this.name = name;
        this.key = name.toLowerCase(Locale.ROOT);
    }

    /** Creates a category from user input or stored data, validating the name. */
    public static Category of(String rawName) {
        if (rawName == null) {
            throw new ValidationException("Category is required.");
        }
        String normalized = rawName.strip().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            throw new ValidationException("Category must not be empty.");
        }
        if (normalized.length() > MAX_LENGTH) {
            throw new ValidationException("Category must be at most " + MAX_LENGTH + " characters long.");
        }
        if (!ALLOWED_CHARACTERS.matcher(normalized).matches()) {
            throw new ValidationException(
                    "Category may only contain letters, digits, spaces, '&', ''' and '-'.");
        }
        return new Category(defaultSpellingOf(normalized));
    }

    /** The suggested categories shown in the menu for the given transaction type. */
    public static List<Category> defaultsFor(TransactionType type) {
        List<String> names = switch (type) {
            case INCOME -> INCOME_DEFAULTS;
            case EXPENSE -> EXPENSE_DEFAULTS;
        };
        return names.stream().map(Category::new).toList();
    }

    private static String defaultSpellingOf(String name) {
        return Stream.concat(INCOME_DEFAULTS.stream(), EXPENSE_DEFAULTS.stream())
                .filter(defaultName -> defaultName.equalsIgnoreCase(name))
                .findFirst()
                .orElse(name);
    }

    public String getName() {
        return name;
    }

    @Override
    public int compareTo(Category other) {
        return key.compareTo(other.key);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Category category && key.equals(category.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
