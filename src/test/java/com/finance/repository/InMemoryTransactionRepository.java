package com.finance.repository;

import com.finance.exception.ValidationException;
import com.finance.model.Transaction;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

/** Test double that keeps transactions in memory only, so service tests never touch the disk. */
public class InMemoryTransactionRepository implements TransactionRepository {

    private final TreeMap<Long, Transaction> transactions = new TreeMap<>();

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
        if (transactions.containsKey(transaction.id())) {
            throw new ValidationException("A transaction with ID " + transaction.id() + " already exists.");
        }
        transactions.put(transaction.id(), transaction);
    }

    @Override
    public boolean deleteById(long id) {
        return transactions.remove(id) != null;
    }

    @Override
    public long nextId() {
        return transactions.isEmpty() ? 1 : transactions.lastKey() + 1;
    }
}
