package com.finance.service;

import com.finance.model.BudgetStatus;
import com.finance.model.Money;
import com.finance.repository.BudgetRepository;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Objects;
import java.util.Optional;

/**
 * Manages a single monthly spending budget that applies to every month
 * and compares it with the actual expenses of a month.
 */
public class BudgetService {

    private final BudgetRepository repository;
    private final FinanceService financeService;
    private BigDecimal monthlyLimit; // null when no budget is set

    /**
     * @throws com.finance.exception.DataAccessException if the stored budget is unreadable
     */
    public BudgetService(BudgetRepository repository, FinanceService financeService) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.financeService = Objects.requireNonNull(financeService, "financeService");
        this.monthlyLimit = repository.loadMonthlyLimit().orElse(null);
    }

    public Optional<BigDecimal> getMonthlyBudget() {
        return Optional.ofNullable(monthlyLimit);
    }

    /**
     * @throws com.finance.exception.ValidationException if the limit is not a positive amount
     */
    public void setMonthlyBudget(BigDecimal limit) {
        BigDecimal validated = Money.requirePositive(limit, "Monthly budget");
        repository.saveMonthlyLimit(validated);
        monthlyLimit = validated; // only update memory after saving succeeded
    }

    public void clearMonthlyBudget() {
        repository.clearMonthlyLimit();
        monthlyLimit = null;
    }

    /** Budget usage for the given month, or empty if no budget is set. */
    public Optional<BudgetStatus> getStatus(YearMonth month) {
        return getMonthlyBudget()
                .map(limit -> new BudgetStatus(month, limit, financeService.getTotalExpenses(month)));
    }
}
