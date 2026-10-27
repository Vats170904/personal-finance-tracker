package com.finance.repository;

import com.finance.exception.DataAccessException;
import com.finance.exception.ValidationException;
import com.finance.model.Money;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * Stores the monthly budget in a small {@code .properties} file:
 *
 * <pre>
 * monthly.limit=500.00
 * </pre>
 */
public class BudgetRepository {

    private static final String MONTHLY_LIMIT_KEY = "monthly.limit";

    private final Path file;

    public BudgetRepository(Path file) {
        this.file = Objects.requireNonNull(file, "file");
    }

    /**
     * @return the stored budget, or empty if none has been set
     * @throws DataAccessException if the file cannot be read or holds an invalid value
     */
    public Optional<BigDecimal> loadMonthlyLimit() {
        if (Files.notExists(file)) {
            return Optional.empty();
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException | IllegalArgumentException e) {
            throw new DataAccessException("Could not read " + file + ": " + e.getMessage(), e);
        }

        String value = properties.getProperty(MONTHLY_LIMIT_KEY);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Money.requirePositive(new BigDecimal(value.strip()), "Monthly budget"));
        } catch (NumberFormatException | ValidationException e) {
            throw new DataAccessException(
                    "Invalid monthly budget '" + value.strip() + "' in " + file + ".", e);
        }
    }

    public void saveMonthlyLimit(BigDecimal limit) {
        Properties properties = new Properties();
        properties.setProperty(MONTHLY_LIMIT_KEY, limit.toPlainString());
        try {
            AtomicFileWriter.write(file, writer -> properties.store(writer, "Personal Finance Tracker budget"));
        } catch (IOException e) {
            throw new DataAccessException("Could not save " + file + ": " + e.getMessage(), e);
        }
    }

    public void clearMonthlyLimit() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new DataAccessException("Could not delete " + file + ": " + e.getMessage(), e);
        }
    }
}
