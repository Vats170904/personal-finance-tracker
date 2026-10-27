package com.finance.ui;

import com.finance.exception.DataAccessException;
import com.finance.exception.ValidationException;
import com.finance.model.BudgetStatus;
import com.finance.model.Category;
import com.finance.model.MonthlySummary;
import com.finance.model.Transaction;
import com.finance.model.TransactionType;
import com.finance.service.BudgetService;
import com.finance.service.FinanceService;
import com.finance.ui.InputReader.CancelledException;
import com.finance.ui.InputReader.EndOfInputException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Terminal user interface: shows menus, reads input and prints results.
 *
 * <p>All calculations are delegated to the services. Input and output streams and the
 * clock are passed in, so the whole UI can be tested with scripted input.
 */
public class ConsoleUI {

    private static final String DOUBLE_LINE = "=".repeat(60);
    private static final String SINGLE_LINE = "-".repeat(60);
    private static final String ROW_FORMAT = "%-5s %-10s %-7s %-16s %14s  %s%n";
    private static final int CATEGORY_COLUMN_WIDTH = 16;
    private static final int DESCRIPTION_COLUMN_WIDTH = 30;

    private enum MenuOption {
        ADD_INCOME(1, "Add Income"),
        ADD_EXPENSE(2, "Add Expense"),
        VIEW_ALL(3, "View All Transactions"),
        VIEW_BY_CATEGORY(4, "View Transactions by Category"),
        VIEW_BY_DATE_RANGE(5, "View Transactions by Date Range"),
        SEARCH(6, "Search Descriptions"),
        MONTHLY_SUMMARY(7, "View Monthly Financial Summary"),
        TOTALS(8, "View Total Income, Expenses and Balance"),
        BUDGET(9, "Set or View Monthly Budget"),
        DELETE(10, "Delete a Transaction"),
        EXPORT(11, "Export Transactions to CSV"),
        EXIT(0, "Exit");

        private final int number;
        private final String label;

        MenuOption(int number, String label) {
            this.number = number;
            this.label = label;
        }

        static MenuOption fromInput(String input) {
            int choice = InputParser.parseMenuChoice(input, 0, values().length - 1);
            for (MenuOption option : values()) {
                if (option.number == choice) {
                    return option;
                }
            }
            throw new ValidationException("Unknown menu option " + choice + ".");
        }
    }

    private final FinanceService financeService;
    private final BudgetService budgetService;
    private final InputReader input;
    private final PrintStream out;
    private final MoneyFormatter moneyFormatter;
    private final Clock clock;
    private final Path exportDirectory;

    public ConsoleUI(FinanceService financeService, BudgetService budgetService, InputReader input,
                     PrintStream out, MoneyFormatter moneyFormatter, Clock clock, Path exportDirectory) {
        this.financeService = Objects.requireNonNull(financeService, "financeService");
        this.budgetService = Objects.requireNonNull(budgetService, "budgetService");
        this.input = Objects.requireNonNull(input, "input");
        this.out = Objects.requireNonNull(out, "out");
        this.moneyFormatter = Objects.requireNonNull(moneyFormatter, "moneyFormatter");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.exportDirectory = Objects.requireNonNull(exportDirectory, "exportDirectory");
    }

    /** Runs the main menu loop until the user chooses Exit or the input ends. */
    public void run() {
        boolean running = true;
        while (running) {
            printMainMenu();
            try {
                MenuOption option = input.readValid("Choose an option: ", MenuOption::fromInput);
                running = handle(option);
            } catch (CancelledException e) {
                out.println("Cancelled. Returning to the main menu.");
            } catch (ValidationException e) {
                out.println("Error: " + e.getMessage());
            } catch (DataAccessException e) {
                out.println("Error: " + e.getMessage());
                out.println("The last change was not saved. Your existing data is unchanged.");
            } catch (EndOfInputException e) {
                running = false;
            }
        }
        out.println("Goodbye!");
    }

    private boolean handle(MenuOption option) {
        out.println();
        switch (option) {
            case ADD_INCOME -> addTransaction(TransactionType.INCOME);
            case ADD_EXPENSE -> addTransaction(TransactionType.EXPENSE);
            case VIEW_ALL -> viewAllTransactions();
            case VIEW_BY_CATEGORY -> viewByCategory();
            case VIEW_BY_DATE_RANGE -> viewByDateRange();
            case SEARCH -> searchDescriptions();
            case MONTHLY_SUMMARY -> showMonthlySummary();
            case TOTALS -> showTotals();
            case BUDGET -> manageBudget();
            case DELETE -> deleteTransaction();
            case EXPORT -> exportTransactions();
            case EXIT -> {
                return false;
            }
        }
        return true;
    }

    // ---- Menu actions ------------------------------------------------------------------

    private void addTransaction(TransactionType type) {
        printHeading("Add " + type.getDisplayName());
        out.println("(Type '" + InputReader.CANCEL_KEYWORD + "' at any prompt to return to the menu.)");

        BigDecimal amount = input.readValid("Amount (e.g. 12.50): ", InputParser::parseAmount);
        Category category = readCategory(type);
        String description = input.readValid("Description (optional): ", Transaction::normalizeDescription);
        LocalDate date = readDateOrDefault("Date (yyyy-MM-dd, Enter for today " + today() + "): ", today());

        Transaction transaction = type == TransactionType.INCOME
                ? financeService.addIncome(amount, category, description, date)
                : financeService.addExpense(amount, category, description, date);

        out.println();
        out.printf("%s recorded: ID %d | %s | %s | %s%n",
                type.getDisplayName(), transaction.id(), money(transaction.amount()),
                transaction.category(), transaction.date());

        if (transaction.isExpense()) {
            budgetService.getStatus(transaction.yearMonth()).ifPresent(this::printBudgetWarning);
        }
    }

    private void viewAllTransactions() {
        printHeading("All Transactions");
        printTransactions(financeService.getAllTransactions());
    }

    private void viewByCategory() {
        printHeading("Transactions by Category");
        List<Category> used = new ArrayList<>(financeService.getUsedCategories());
        if (used.isEmpty()) {
            out.println("No transactions recorded yet.");
            return;
        }
        out.println("Categories with transactions:");
        printNumbered(used);
        Category category = input.readValid("Category (number or name): ", text -> chooseCategory(text, used));

        List<Transaction> matches = financeService.findByCategory(category);
        printTransactions(matches);
        printTotalsFor(matches);
    }

    private void viewByDateRange() {
        printHeading("Transactions by Date Range");
        LocalDate from = input.readValid("Start date (yyyy-MM-dd): ", InputParser::parseDate);
        LocalDate to = readDateOrDefault("End date (yyyy-MM-dd, Enter for today " + today() + "): ", today());

        List<Transaction> matches = financeService.findByDateRange(from, to);
        out.println("From " + from + " to " + to + ":");
        printTransactions(matches);
        printTotalsFor(matches);
    }

    private void searchDescriptions() {
        printHeading("Search Descriptions");
        String query = input.readValid("Search text: ", text -> {
            if (text.isBlank()) {
                throw new ValidationException("Search text must not be empty.");
            }
            return text;
        });
        printTransactions(financeService.searchDescriptions(query));
    }

    private void showMonthlySummary() {
        printHeading("Monthly Financial Summary");
        YearMonth currentMonth = YearMonth.now(clock);
        YearMonth month = input.readValid("Month (yyyy-MM, Enter for " + currentMonth + "): ",
                text -> text.isEmpty() ? currentMonth : InputParser.parseYearMonth(text));

        MonthlySummary summary = financeService.getMonthlySummary(month);
        out.println();
        out.println("Summary for " + month + " (" + summary.transactionCount() + " transaction(s))");
        out.println(SINGLE_LINE);
        printLabeledAmount("Income", summary.totalIncome());
        printLabeledAmount("Expenses", summary.totalExpenses());
        printLabeledAmount("Savings", summary.savings());
        printExpenseBreakdown(summary.expensesByCategory(), summary.totalExpenses());

        budgetService.getStatus(month).ifPresent(status -> {
            out.println();
            printBudgetStatus(status);
        });

        if (summary.transactionCount() > 0) {
            out.println();
            printTransactions(financeService.findByMonth(month));
        }
    }

    private void showTotals() {
        printHeading("Totals (all time)");
        BigDecimal totalExpenses = financeService.getTotalExpenses();
        printLabeledAmount("Total income", financeService.getTotalIncome());
        printLabeledAmount("Total expenses", totalExpenses);
        printLabeledAmount("Balance", financeService.getBalance());
        printExpenseBreakdown(financeService.getExpensesByCategory(), totalExpenses);
    }

    private void manageBudget() {
        printHeading("Monthly Budget");
        YearMonth currentMonth = YearMonth.now(clock);
        Optional<BudgetStatus> currentStatus = budgetService.getStatus(currentMonth);
        if (currentStatus.isPresent()) {
            printBudgetStatus(currentStatus.get());
        } else {
            out.println("No monthly budget set.");
        }

        out.println();
        out.println("  1. Set or change monthly budget");
        out.println("  2. View budget status for another month");
        out.println("  3. Remove monthly budget");
        out.println("  0. Back to main menu");
        int choice = input.readValid("Choose an option: ", text -> InputParser.parseMenuChoice(text, 0, 3));

        switch (choice) {
            case 1 -> setBudget(currentMonth);
            case 2 -> showBudgetForMonth();
            case 3 -> removeBudget();
            default -> { /* back to main menu */ }
        }
    }

    private void setBudget(YearMonth currentMonth) {
        BigDecimal limit = input.readValid("New monthly budget: ", InputParser::parseAmount);
        budgetService.setMonthlyBudget(limit);
        out.println("Monthly budget set to " + money(limit) + ".");
        budgetService.getStatus(currentMonth).ifPresent(this::printBudgetWarning);
    }

    private void showBudgetForMonth() {
        if (budgetService.getMonthlyBudget().isEmpty()) {
            out.println("Please set a monthly budget first.");
            return;
        }
        YearMonth month = input.readValid("Month (yyyy-MM): ", InputParser::parseYearMonth);
        budgetService.getStatus(month).ifPresent(this::printBudgetStatus);
    }

    private void removeBudget() {
        if (budgetService.getMonthlyBudget().isEmpty()) {
            out.println("There is no budget to remove.");
            return;
        }
        if (input.confirm("Remove the monthly budget? (y/n): ")) {
            budgetService.clearMonthlyBudget();
            out.println("Monthly budget removed.");
        } else {
            out.println("Budget kept.");
        }
    }

    private void deleteTransaction() {
        printHeading("Delete a Transaction");
        long id = input.readValid("Transaction ID to delete: ", InputParser::parseId);
        Optional<Transaction> transaction = financeService.findById(id);
        if (transaction.isEmpty()) {
            out.println("No transaction with ID " + id + " exists. Use option 3 to see all IDs.");
            return;
        }
        printTransactions(List.of(transaction.get()));
        if (input.confirm("Delete this transaction? (y/n): ")) {
            financeService.deleteTransaction(id);
            out.println("Transaction " + id + " deleted.");
        } else {
            out.println("Nothing was deleted.");
        }
    }

    private void exportTransactions() {
        printHeading("Export Transactions to CSV");
        Path defaultPath = exportDirectory.resolve("transactions-export-" + today() + ".csv");
        Path target = input.readValid("File path (Enter for " + defaultPath + "): ",
                text -> text.isEmpty() ? defaultPath : InputParser.parseCsvPath(text));

        if (Files.exists(target) && !input.confirm("The file already exists. Overwrite it? (y/n): ")) {
            out.println("Export cancelled.");
            return;
        }
        int count = financeService.exportToCsv(target);
        out.println("Exported " + count + " transaction(s) to " + target.toAbsolutePath());
    }

    // ---- Input helpers -----------------------------------------------------------------

    private Category readCategory(TransactionType type) {
        List<Category> defaults = Category.defaultsFor(type);
        out.println("Categories:");
        printNumbered(defaults);
        out.println("  Or type the name of a custom category.");
        return input.readValid("Category: ", text -> chooseCategory(text, defaults));
    }

    /** Interprets input as a list number if it is numeric, otherwise as a category name. */
    private static Category chooseCategory(String text, List<Category> options) {
        if (!text.isEmpty() && text.chars().allMatch(Character::isDigit)) {
            int choice = InputParser.parseMenuChoice(text, 1, options.size());
            return options.get(choice - 1);
        }
        return Category.of(text);
    }

    private LocalDate readDateOrDefault(String prompt, LocalDate defaultDate) {
        return input.readValid(prompt, text -> text.isEmpty() ? defaultDate : InputParser.parseDate(text));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    // ---- Output helpers ----------------------------------------------------------------

    private void printMainMenu() {
        out.println();
        out.println(DOUBLE_LINE);
        out.println("  PERSONAL FINANCE TRACKER");
        out.println(DOUBLE_LINE);
        for (MenuOption option : MenuOption.values()) {
            out.printf("  %2d. %s%n", option.number, option.label);
        }
        out.println(DOUBLE_LINE);
    }

    private void printHeading(String title) {
        out.println("--- " + title + " ---");
    }

    private void printNumbered(List<Category> categories) {
        for (int i = 0; i < categories.size(); i++) {
            out.printf("  %d. %s%n", i + 1, categories.get(i));
        }
    }

    private void printTransactions(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            out.println("No transactions found.");
            return;
        }
        out.printf(ROW_FORMAT, "ID", "Date", "Type", "Category", "Amount", "Description");
        out.println(SINGLE_LINE);
        for (Transaction t : transactions) {
            out.printf(ROW_FORMAT,
                    t.id(),
                    t.date(),
                    t.type().getDisplayName(),
                    truncate(t.category().getName(), CATEGORY_COLUMN_WIDTH),
                    money(t.amount()),
                    truncate(t.description(), DESCRIPTION_COLUMN_WIDTH));
        }
        out.println(SINGLE_LINE);
        out.println(transactions.size() + " transaction(s).");
    }

    private void printTotalsFor(List<Transaction> transactions) {
        if (transactions.isEmpty()) {
            return;
        }
        BigDecimal income = transactions.stream().filter(Transaction::isIncome)
                .map(Transaction::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expenses = transactions.stream().filter(Transaction::isExpense)
                .map(Transaction::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        out.println("Income: " + money(income) + " | Expenses: " + money(expenses)
                + " | Net: " + money(income.subtract(expenses)));
    }

    private void printExpenseBreakdown(Map<Category, BigDecimal> byCategory, BigDecimal totalExpenses) {
        if (byCategory.isEmpty()) {
            return;
        }
        out.println();
        out.println("Expenses by category:");
        byCategory.forEach((category, amount) -> out.printf("  %-18s %14s  (%s%%)%n",
                truncate(category.getName(), 18), money(amount), percentOf(amount, totalExpenses)));
    }

    private void printBudgetStatus(BudgetStatus status) {
        out.println("Budget for " + status.month());
        printLabeledAmount("Monthly budget", status.limit());
        printLabeledAmount("Spent", status.spent());
        printLabeledAmount("Remaining", status.remaining());
        out.println("  Used: " + status.percentUsed() + "%");
        printBudgetWarning(status);
    }

    private void printBudgetWarning(BudgetStatus status) {
        if (status.isExceeded()) {
            out.println("WARNING: You have exceeded your " + status.month() + " budget by "
                    + money(status.remaining().negate()) + ".");
        } else if (status.isLimitReached()) {
            out.println("WARNING: You have reached your " + status.month() + " budget.");
        } else if (status.isNearLimit()) {
            out.println("Note: You have used " + status.percentUsed() + "% of your " + status.month()
                    + " budget. " + money(status.remaining()) + " left.");
        }
    }

    private void printLabeledAmount(String label, BigDecimal amount) {
        out.printf("  %-16s %14s%n", label + ":", money(amount));
    }

    private String money(BigDecimal amount) {
        return moneyFormatter.format(amount);
    }

    private static String percentOf(BigDecimal part, BigDecimal whole) {
        if (whole.signum() == 0) {
            return "0.0";
        }
        return part.multiply(BigDecimal.valueOf(100)).divide(whole, 1, RoundingMode.HALF_UP).toPlainString();
    }

    private static String truncate(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
    }
}
