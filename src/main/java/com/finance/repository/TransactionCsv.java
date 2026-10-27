package com.finance.repository;

import com.finance.exception.DataAccessException;
import com.finance.exception.ValidationException;
import com.finance.model.Category;
import com.finance.model.Transaction;
import com.finance.model.TransactionType;
import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reads and writes transactions in CSV format:
 *
 * <pre>
 * id,type,date,amount,category,description
 * 1,EXPENSE,2026-10-20,12.50,Food,"Lunch, with friends"
 * </pre>
 *
 * Used both for the application's data file and for user exports.
 */
public final class TransactionCsv {

    static final String HEADER = "id,type,date,amount,category,description";

    private static final int FIELD_COUNT = 6;
    private static final char BYTE_ORDER_MARK = '﻿';

    private TransactionCsv() {
        // utility class
    }

    /**
     * Reads all transactions from {@code file}. A missing or empty file yields an empty list.
     *
     * @throws DataAccessException if the file cannot be read or contains an invalid record;
     *                             the message includes the line number
     */
    public static List<Transaction> read(Path file) {
        if (Files.notExists(file)) {
            return List.of();
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            List<Transaction> transactions = new ArrayList<>();
            Set<Long> seenIds = new HashSet<>();
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1) {
                    line = stripByteOrderMark(line);
                }
                if (line.isBlank() || (lineNumber == 1 && line.strip().equals(HEADER))) {
                    continue;
                }
                Transaction transaction = parseRecord(line, file, lineNumber);
                if (!seenIds.add(transaction.id())) {
                    throw new DataAccessException(location(file, lineNumber)
                            + "duplicate transaction ID " + transaction.id() + ".");
                }
                transactions.add(transaction);
            }
            return transactions;
        } catch (IOException e) {
            throw new DataAccessException("Could not read " + file + ": " + e.getMessage(), e);
        }
    }

    /**
     * Writes the transactions to {@code file}, replacing it atomically.
     * Parent directories are created if necessary.
     */
    public static void write(Path file, Collection<Transaction> transactions) {
        try {
            AtomicFileWriter.write(file, writer -> {
                writer.write(HEADER);
                writer.newLine();
                for (Transaction transaction : transactions) {
                    writer.write(toRecord(transaction));
                    writer.newLine();
                }
            });
        } catch (IOException e) {
            throw new DataAccessException("Could not save " + file + ": " + e.getMessage(), e);
        }
    }

    static String toRecord(Transaction t) {
        return CsvLine.format(List.of(
                String.valueOf(t.id()),
                t.type().name(),
                t.date().toString(), // ISO 8601: yyyy-MM-dd
                t.amount().toPlainString(),
                t.category().getName(),
                t.description()));
    }

    private static Transaction parseRecord(String line, Path file, int lineNumber) {
        try {
            List<String> fields = CsvLine.parse(line);
            if (fields.size() != FIELD_COUNT) {
                throw new ValidationException(
                        "expected " + FIELD_COUNT + " fields but found " + fields.size());
            }
            long id = Long.parseLong(fields.get(0).strip());
            TransactionType type = TransactionType.fromString(fields.get(1));
            LocalDate date = LocalDate.parse(fields.get(2).strip());
            BigDecimal amount = new BigDecimal(fields.get(3).strip());
            Category category = Category.of(fields.get(4));
            String description = fields.get(5);
            // The Transaction constructor re-validates everything, so a manually edited
            // file cannot introduce e.g. negative amounts.
            return new Transaction(id, type, amount, category, description, date);
        } catch (NumberFormatException e) {
            throw new DataAccessException(location(file, lineNumber) + "invalid number in record.", e);
        } catch (DateTimeParseException e) {
            throw new DataAccessException(location(file, lineNumber) + "invalid date '"
                    + e.getParsedString() + "', expected yyyy-MM-dd.", e);
        } catch (ValidationException e) {
            throw new DataAccessException(location(file, lineNumber) + e.getMessage(), e);
        }
    }

    private static String stripByteOrderMark(String line) {
        // Files saved by Excel on Windows often start with an invisible byte order mark.
        return !line.isEmpty() && line.charAt(0) == BYTE_ORDER_MARK ? line.substring(1) : line;
    }

    private static String location(Path file, int lineNumber) {
        return "Invalid data in " + file + " at line " + lineNumber + ": ";
    }
}
