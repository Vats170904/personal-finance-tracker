package com.finance.repository;

import com.finance.model.Transaction;
import java.util.List;
import java.util.Optional;

/**
 * Storage for transactions. The service layer only depends on this interface,
 * so the CSV implementation can be swapped (e.g. for an in-memory one in tests,
 * or a database later) without changing any business logic.
 */
public interface TransactionRepository {

    /** All stored transactions, in no particular order. The returned list is read-only. */
    List<Transaction> findAll();

    Optional<Transaction> findById(long id);

    /**
     * Stores a new transaction.
     *
     * @throws com.finance.exception.ValidationException if the ID is already in use
     * @throws com.finance.exception.DataAccessException if the data could not be saved
     */
    void add(Transaction transaction);

    /**
     * Removes the transaction with the given ID.
     *
     * @return {@code true} if a transaction was removed, {@code false} if none had this ID
     * @throws com.finance.exception.DataAccessException if the change could not be saved
     */
    boolean deleteById(long id);

    /** The ID to use for the next new transaction. */
    long nextId();
}
