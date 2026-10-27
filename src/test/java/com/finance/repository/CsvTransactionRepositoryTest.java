package com.finance.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.finance.exception.DataAccessException;
import com.finance.exception.ValidationException;
import com.finance.model.Category;
import com.finance.model.Transaction;
import com.finance.model.TransactionType;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** All tests work in a temporary directory that JUnit deletes afterwards. */
class CsvTransactionRepositoryTest {

    @TempDir
    Path tempDir;

    private Path file;

    @BeforeEach
    void setUp() {
        file = tempDir.resolve("transactions.csv");
    }

    private static Transaction transaction(long id, TransactionType type, String amount,
                                           String category, String description, LocalDate date) {
        return new Transaction(id, type, new BigDecimal(amount), Category.of(category), description, date);
    }

    private static Transaction lunch(long id) {
        return transaction(id, TransactionType.EXPENSE, "12.50", "Food", "Lunch", LocalDate.of(2026, 10, 20));
    }

    private void writeFile(String... lines) throws IOException {
        Files.write(file, List.of(lines), StandardCharsets.UTF_8);
    }

    // ---- Loading -----------------------------------------------------------------------

    @Test
    void missingFileStartsEmptyAndIsNotCreatedYet() {
        CsvTransactionRepository repository = new CsvTransactionRepository(file);

        assertTrue(repository.findAll().isEmpty());
        assertEquals(1, repository.nextId());
        assertFalse(Files.exists(file));
    }

    @Test
    void emptyFileAndHeaderOnlyFileAreValid() throws IOException {
        writeFile();
        assertTrue(new CsvTransactionRepository(file).findAll().isEmpty());

        writeFile(TransactionCsv.HEADER, "");
        assertTrue(new CsvTransactionRepository(file).findAll().isEmpty());
    }

    @Test
    void fileWithByteOrderMarkIsAccepted() throws IOException {
        writeFile('﻿' + TransactionCsv.HEADER, "1,EXPENSE,2026-10-20,12.50,Food,Lunch");

        assertEquals(List.of(lunch(1)), new CsvTransactionRepository(file).findAll());
    }

    // ---- Persistence -------------------------------------------------------------------

    @Test
    void transactionsSurviveRestart() {
        Transaction salary = transaction(1, TransactionType.INCOME, "1200", "Salary",
                "October, \"main\" job", LocalDate.of(2026, 10, 1));
        Transaction coffee = transaction(2, TransactionType.EXPENSE, "3.20", "Café & Bäckerei",
                "Coffee ☕ ", LocalDate.of(2026, 10, 2));

        CsvTransactionRepository repository = new CsvTransactionRepository(file);
        repository.add(salary);
        repository.add(coffee);

        CsvTransactionRepository restarted = new CsvTransactionRepository(file);
        assertEquals(List.of(salary, coffee), restarted.findAll());
        assertEquals(3, restarted.nextId());
    }

    @Test
    void deletionSurvivesRestart() {
        CsvTransactionRepository repository = new CsvTransactionRepository(file);
        repository.add(lunch(1));
        repository.add(lunch(2));

        assertTrue(repository.deleteById(1));
        assertFalse(repository.deleteById(99));

        assertEquals(List.of(lunch(2)), new CsvTransactionRepository(file).findAll());
    }

    @Test
    void duplicateIdIsRejected() {
        CsvTransactionRepository repository = new CsvTransactionRepository(file);
        repository.add(lunch(1));

        assertThrows(ValidationException.class, () -> repository.add(lunch(1)));
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void nextIdFollowsHighestId() {
        CsvTransactionRepository repository = new CsvTransactionRepository(file);
        repository.add(lunch(7));

        assertEquals(8, repository.nextId());
    }

    @Test
    void savingCreatesMissingDirectoriesAndLeavesNoTemporaryFiles() throws IOException {
        Path nested = tempDir.resolve("nested").resolve("data").resolve("transactions.csv");
        new CsvTransactionRepository(nested).add(lunch(1));

        assertTrue(Files.exists(nested));
        try (Stream<Path> files = Files.list(nested.getParent())) {
            assertEquals(List.of(nested), files.toList());
        }
    }

    @Test
    void savedFileHasHeaderAndQuotedDescriptions() throws IOException {
        new CsvTransactionRepository(file).add(transaction(1, TransactionType.EXPENSE, "5",
                "Food", "Pizza, \"large\"", LocalDate.of(2026, 10, 20)));

        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        assertEquals(List.of(TransactionCsv.HEADER, "1,EXPENSE,2026-10-20,5.00,Food,\"Pizza, \"\"large\"\"\""), lines);
    }

    // ---- Corrupted data ----------------------------------------------------------------

    @Test
    void corruptedLineIsReportedWithLineNumberAndFileIsUntouched() throws IOException {
        writeFile(TransactionCsv.HEADER,
                "1,EXPENSE,2026-10-20,12.50,Food,Lunch",
                "abc,EXPENSE,2026-10-21,5.00,Food,Snack");
        String before = Files.readString(file);

        DataAccessException error = assertThrows(DataAccessException.class, () -> new CsvTransactionRepository(file));

        assertTrue(error.getMessage().contains("line 3"), error.getMessage());
        assertEquals(before, Files.readString(file));
    }

    @Test
    void invalidValuesInFileAreRejected() throws IOException {
        String[] invalidRecords = {
            "1,EXPENSE,2026-02-30,12.50,Food,Impossible date",
            "1,EXPENSE,20.10.2026,12.50,Food,Wrong date format",
            "1,EXPENSE,2026-10-20,-12.50,Food,Negative amount",
            "1,EXPENSE,2026-10-20,twelve,Food,Not a number",
            "1,TRANSFER,2026-10-20,12.50,Food,Unknown type",
            "1,EXPENSE,2026-10-20,12.50,Food",
            "1,EXPENSE,2026-10-20,12.50,Food,\"Unterminated quote",
        };
        for (String record : invalidRecords) {
            writeFile(TransactionCsv.HEADER, record);
            assertThrows(DataAccessException.class, () -> new CsvTransactionRepository(file), record);
        }
    }

    @Test
    void duplicateIdInFileIsRejected() throws IOException {
        writeFile(TransactionCsv.HEADER,
                "1,EXPENSE,2026-10-20,12.50,Food,Lunch",
                "1,EXPENSE,2026-10-21,5.00,Food,Snack");

        assertThrows(DataAccessException.class, () -> new CsvTransactionRepository(file));
    }
}
