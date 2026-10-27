package com.finance.service;

import com.finance.exception.ValidationException;
import com.finance.model.Category;
import com.finance.model.Money;
import com.finance.model.MonthlySummary;
import com.finance.model.Transaction;
import com.finance.model.TransactionType;
import com.finance.repository.TransactionCsv;
import com.finance.repository.TransactionRepository;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Business logic for recording transactions and calculating totals.
 *
 * <p>This class knows nothing about the terminal or the file format, which makes it
 * easy to unit test with an in-memory repository.
 */
public class FinanceService {

    /** Oldest first; transactions on the same day are ordered by ID. */
    private static final Comparator<Transaction> CHRONOLOGICAL =
            Comparator.comparing(Transaction::date).thenComparingLong(Transaction::id);

    private final TransactionRepository repository;

    public FinanceService(TransactionRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    // ---- Recording and deleting ------------------------------------------------------

    public Transaction addIncome(BigDecimal amount, Category category, String description, LocalDate date) {
        return addTransaction(TransactionType.INCOME, amount, category, description, date);
    }

    public Transaction addExpense(BigDecimal amount, Category category, String description, LocalDate date) {
        return addTransaction(TransactionType.EXPENSE, amount, category, description, date);
    }

    private Transaction addTransaction(TransactionType type, BigDecimal amount, Category category,
                                       String description, LocalDate date) {
        Transaction transaction = new Transaction(repository.nextId(), type, amount, category, description, date);
        repository.add(transaction);
        return transaction;
    }

    /** @return {@code true} if the transaction existed and was deleted */
    public boolean deleteTransaction(long id) {
        return repository.deleteById(id);
    }

    public Optional<Transaction> findById(long id) {
        return repository.findById(id);
    }

    // ---- Querying ----------------------------------------------------------------------

    public List<Transaction> getAllTransactions() {
        return sorted(repository.findAll().stream());
    }

    public List<Transaction> findByCategory(Category category) {
        Objects.requireNonNull(category, "category");
        return sorted(repository.findAll().stream().filter(t -> t.category().equals(category)));
    }

    /** Transactions between {@code from} and {@code to}, both dates included. */
    public List<Transaction> findByDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new ValidationException("Both a start date and an end date are required.");
        }
        if (from.isAfter(to)) {
            throw new ValidationException("Start date " + from + " is after end date " + to + ".");
        }
        return sorted(repository.findAll().stream()
                .filter(t -> !t.date().isBefore(from) && !t.date().isAfter(to)));
    }

    public List<Transaction> findByMonth(YearMonth month) {
        Objects.requireNonNull(month, "month");
        return sorted(repository.findAll().stream().filter(t -> t.yearMonth().equals(month)));
    }

    /** Case-insensitive search in transaction descriptions. */
    public List<Transaction> searchDescriptions(String query) {
        if (query == null || query.isBlank()) {
            throw new ValidationException("Search text must not be empty.");
        }
        String needle = query.strip().toLowerCase(Locale.ROOT);
        return sorted(repository.findAll().stream()
                .filter(t -> t.description().toLowerCase(Locale.ROOT).contains(needle)));
    }

    /** Every category that has at least one transaction, sorted alphabetically. */
    public SortedSet<Category> getUsedCategories() {
        return repository.findAll().stream()
                .map(Transaction::category)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    // ---- Calculations ------------------------------------------------------------------

    public BigDecimal getTotalIncome() {
        return sum(repository.findAll(), TransactionType.INCOME);
    }

    public BigDecimal getTotalExpenses() {
        return sum(repository.findAll(), TransactionType.EXPENSE);
    }

    /** Total income minus total expenses. */
    public BigDecimal getBalance() {
        return getTotalIncome().subtract(getTotalExpenses());
    }

    public BigDecimal getTotalExpenses(YearMonth month) {
        return sum(findByMonth(month), TransactionType.EXPENSE);
    }

    /** Expense totals per category over all time, largest first. */
    public Map<Category, BigDecimal> getExpensesByCategory() {
        return expensesByCategory(repository.findAll());
    }

    public MonthlySummary getMonthlySummary(YearMonth month) {
        List<Transaction> monthly = findByMonth(month);
        return new MonthlySummary(
                month,
                sum(monthly, TransactionType.INCOME),
                sum(monthly, TransactionType.EXPENSE),
                expensesByCategory(monthly),
                monthly.size());
    }

    // ---- Export ------------------------------------------------------------------------

    /**
     * Writes all transactions, oldest first, to a CSV file.
     *
     * @return the number of exported transactions
     */
    public int exportToCsv(Path target) {
        List<Transaction> all = getAllTransactions();
        TransactionCsv.write(target, all);
        return all.size();
    }

    // ---- Helpers -----------------------------------------------------------------------

    private static List<Transaction> sorted(Stream<Transaction> transactions) {
        return transactions.sorted(CHRONOLOGICAL).toList();
    }

    private static BigDecimal sum(Collection<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(t -> t.type() == type)
                .map(Transaction::amount)
                .reduce(Money.ZERO, BigDecimal::add);
    }

    private static Map<Category, BigDecimal> expensesByCategory(Collection<Transaction> transactions) {
        Map<Category, BigDecimal> totals = transactions.stream()
                .filter(Transaction::isExpense)
                .collect(Collectors.groupingBy(
                        Transaction::category,
                        Collectors.reducing(Money.ZERO, Transaction::amount, BigDecimal::add)));

        Comparator<Map.Entry<Category, BigDecimal>> largestFirst =
                Map.Entry.<Category, BigDecimal>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey());

        Map<Category, BigDecimal> sortedTotals = new LinkedHashMap<>();
        totals.entrySet().stream()
                .sorted(largestFirst)
                .forEachOrdered(entry -> sortedTotals.put(entry.getKey(), entry.getValue()));
        return Collections.unmodifiableMap(sortedTotals);
    }
}
