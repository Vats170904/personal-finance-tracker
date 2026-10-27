package com.finance.repository;

import com.finance.exception.DataAccessException;
import com.finance.exception.ValidationException;
import com.finance.model.Transaction;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Stores transactions in a CSV file.
 *
 * <p>All transactions are loaded into memory when the repository is created.
 * Every change is written back to the file immediately, so nothing is lost when the
 * program closes. If saving fails, the in-memory change is rolled back so memory and
 * file stay consistent.
 */
public class CsvTransactionRepository implements TransactionRepository {

    private final Path file;
    // TreeMap keeps transactions sorted by ID, so the file is written in a stable order.
    private final TreeMap<Long, Transaction> transactions = new TreeMap<>();

    /**
     * @throws DataAccessException if the existing file cannot be read or is corrupted
     */
    public CsvTransactionRepository(Path file) {
        this.file = Objects.requireNonNull(file, "file");
        for (Transaction transaction : TransactionCsv.read(file)) {
            transactions.put(transaction.id(), transaction);
        }
    }

    @Override
    public List<Transaction> findAll() {
        return List.copyOf(transactions.values());
    }

    @Override
    public Optional<Transaction> findById(long id) {
        return Optional.ofNullable(transactions.get(id));
    }

    @Override
    public void add(Transaction transaction) {
        Objects.requireNonNull(transaction, "transaction");
        if (transactions.containsKey(transaction.id())) {
            throw new ValidationException("A transaction with ID " + transaction.id() + " already exists.");
        }
        transactions.put(transaction.id(), transaction);
        try {
            persist();
        } catch (DataAccessException e) {
            transactions.remove(transaction.id());
            throw e;
        }
    }

    @Override
    public boolean deleteById(long id) {
        Transaction removed = transactions.remove(id);
        if (removed == null) {
            return false;
        }
        try {
            persist();
        } catch (DataAccessException e) {
            transactions.put(id, removed);
            throw e;
        }
        return true;
    }

    @Override
    public long nextId() {
        return transactions.isEmpty() ? 1 : transactions.lastKey() + 1;
    }

    private void persist() {
        TransactionCsv.write(file, transactions.values());
    }
}
