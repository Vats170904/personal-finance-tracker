package com.finance;

import com.finance.exception.DataAccessException;
import com.finance.repository.BudgetRepository;
import com.finance.repository.CsvTransactionRepository;
import com.finance.service.BudgetService;
import com.finance.service.FinanceService;
import com.finance.ui.ConsoleUI;
import com.finance.ui.InputReader;
import com.finance.ui.MoneyFormatter;
import java.io.Console;
import java.nio.charset.Charset;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Clock;

/**
 * Application entry point. Creates the objects, connects them and starts the UI.
 *
 * <p>Usage: {@code java -jar personal-finance-tracker.jar [data-directory]}.
 * The data directory defaults to {@code ./data}.
 */
public final class Main {

    private static final String DEFAULT_DATA_DIRECTORY = "data";
    private static final String TRANSACTIONS_FILE = "transactions.csv";
    private static final String BUDGET_FILE = "budget.properties";
    private static final String CURRENCY_PROPERTY = "finance.currency";
    private static final String DEFAULT_CURRENCY_SYMBOL = "€";
    private static final String FALLBACK_CURRENCY_SYMBOL = "EUR";

    private Main() {
    }

    public static void main(String[] args) {
        if (args.length > 0 && (args[0].equals("--help") || args[0].equals("-h"))) {
            printUsage();
            return;
        }

        try {
            Path dataDirectory = Path.of(args.length > 0 ? args[0] : DEFAULT_DATA_DIRECTORY);

            FinanceService financeService =
                    new FinanceService(new CsvTransactionRepository(dataDirectory.resolve(TRANSACTIONS_FILE)));
            BudgetService budgetService =
                    new BudgetService(new BudgetRepository(dataDirectory.resolve(BUDGET_FILE)), financeService);
            InputReader inputReader = new InputReader(System.in, consoleCharset(), System.out);
            MoneyFormatter moneyFormatter =
                    new MoneyFormatter(System.getProperty(CURRENCY_PROPERTY, defaultCurrencySymbol()));

            System.out.println("Data directory: " + dataDirectory.toAbsolutePath());
            new ConsoleUI(financeService, budgetService, inputReader, System.out,
                    moneyFormatter, Clock.systemDefaultZone(), dataDirectory).run();

        } catch (InvalidPathException e) {
            System.err.println("Invalid data directory: " + e.getMessage());
            System.exit(1);
        } catch (DataAccessException e) {
            // Stop before anything is written, so a damaged file is never overwritten.
            System.err.println("Could not load your data: " + e.getMessage());
            System.err.println("No files were changed. Please fix or move the file and start again.");
            System.exit(1);
        }
    }

    /** Uses the terminal's own encoding when available, so non-ASCII input is read correctly. */
    private static Charset consoleCharset() {
        Console console = System.console();
        return console != null ? console.charset() : Charset.defaultCharset();
    }

    /**
     * Returns "€" if the terminal's output encoding can display it, otherwise "EUR".
     * Some Windows consoles (code page 850/437) would print "?" instead of the euro sign.
     */
    private static String defaultCurrencySymbol() {
        // "stdout.encoding" exists from Java 19; "sun.stdout.encoding" is set by Java 17 on Windows consoles.
        String encoding = System.getProperty("stdout.encoding", System.getProperty("sun.stdout.encoding"));
        Charset charset = Charset.defaultCharset();
        try {
            if (encoding != null && Charset.isSupported(encoding)) {
                charset = Charset.forName(encoding);
            }
        } catch (IllegalArgumentException e) {
            // Unknown encoding name: keep the default charset.
        }
        return charset.newEncoder().canEncode(DEFAULT_CURRENCY_SYMBOL) ? DEFAULT_CURRENCY_SYMBOL : FALLBACK_CURRENCY_SYMBOL;
    }

    private static void printUsage() {
        System.out.println("Personal Finance Tracker");
        System.out.println();
        System.out.println("Usage: java -jar target/personal-finance-tracker.jar [data-directory]");
        System.out.println();
        System.out.println("  data-directory  Where transactions.csv and budget.properties are stored.");
        System.out.println("                  Default: ./" + DEFAULT_DATA_DIRECTORY);
        System.out.println();
        System.out.println("Options (JVM system properties):");
        System.out.println("  -D" + CURRENCY_PROPERTY + "=EUR  Currency symbol to display (default: the euro sign, or "
                + FALLBACK_CURRENCY_SYMBOL + " if the terminal cannot show it)");
    }
}
