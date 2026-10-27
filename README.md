# Personal Finance Tracker

A command-line application for recording income and expenses, tracking a monthly budget
and summarizing spending — written in plain Java 17 with Maven and JUnit 5.

![Build](https://github.com/Vats170904/personal-finance-tracker/actions/workflows/build.yml/badge.svg)

## Overview

The tracker runs entirely in the terminal. Transactions are saved to a local CSV file
after every change, so data is kept between sessions. All money calculations use
`BigDecimal`, so results are exact to the cent.

## Motivation and goals

I built this project to practise writing a complete, maintainable Java application
rather than isolated exercises. The goals were:

- a clean, layered architecture with clearly separated responsibilities
- correct financial calculations without floating-point errors
- robust input validation, so the program never crashes on unexpected input
- reliable file storage that cannot corrupt existing data
- meaningful unit tests for the business logic, storage and user interface

## Features

- Record **income** and **expenses** with amount, category, description and date
- Default categories (Salary, Scholarship, Food, Rent, Transportation, Education,
  Shopping, Entertainment, Healthcare, Other) plus **custom categories**
- View all transactions, or filter by **category**, **date range** or **month**
- **Search** transaction descriptions
- **Monthly summary**: income, expenses, savings and spending per category
- **Totals**: overall income, expenses, balance and spending per category
- **Monthly budget** with remaining amount and warnings at 80 %, 100 % and above
- **Delete** transactions (with confirmation)
- **Export** all transactions to a CSV file that opens in Excel or LibreOffice
- Accepts both `12.50` and `12,50` as amounts
- Type `cancel` at any prompt to go back to the main menu

## Technologies

| Technology | Purpose |
|---|---|
| Java 17 | Language (records, switch expressions, `java.time`, `BigDecimal`) |
| Maven | Build, dependency management, packaging |
| JUnit 5 | Unit tests (the only dependency, test scope only) |
| GitHub Actions | Builds and tests on Linux, Windows and macOS with Java 17 and 21 |

The application itself has **no runtime dependencies**: CSV handling and input parsing
are implemented with the Java standard library.

## Prerequisites

- **JDK 17 or newer** (for example [Eclipse Temurin](https://adoptium.net/))
- **Apache Maven 3.6.3 or newer** ([installation guide](https://maven.apache.org/install.html))
- **Git** (to clone the repository)

Check what is installed:

```bash
java -version     # must show version 17 or higher
javac -version    # confirms a full JDK (not only a JRE) is installed
mvn -version      # shows the Maven version and which Java it uses
```

If `mvn` is not found, install Maven (macOS: `brew install maven`, Ubuntu/Debian:
`sudo apt install maven`, Windows: download the binary zip and add its `bin` folder
to `PATH`, or `winget install Apache.Maven` / `choco install maven`).

## Installation

```bash
git clone https://github.com/Vats170904/personal-finance-tracker.git
cd personal-finance-tracker
```

## Build and run

```bash
# Compile, run all tests and package the application
mvn clean package

# Start the application from the packaged JAR
java -jar target/personal-finance-tracker.jar
```

Alternatively, run it directly through Maven (after `mvn compile`):

```bash
mvn -q compile exec:java
```

Options:

```bash
# Store data in another folder (default: ./data)
java -jar target/personal-finance-tracker.jar /path/to/my-data

# Show "EUR" instead of the euro sign
java -Dfinance.currency=EUR -jar target/personal-finance-tracker.jar

# Show help
java -jar target/personal-finance-tracker.jar --help
```

> **Windows note:** if your console cannot display `€`, the application automatically
> shows `EUR` instead. Running `chcp 65001` first switches the console to UTF-8.

## Example session

```text
============================================================
  PERSONAL FINANCE TRACKER
============================================================
   1. Add Income
   2. Add Expense
   3. View All Transactions
   4. View Transactions by Category
   5. View Transactions by Date Range
   6. Search Descriptions
   7. View Monthly Financial Summary
   8. View Total Income, Expenses and Balance
   9. Set or View Monthly Budget
  10. Delete a Transaction
  11. Export Transactions to CSV
   0. Exit
============================================================
Choose an option: 2

--- Add Expense ---
(Type 'cancel' at any prompt to return to the menu.)
Amount (e.g. 12.50): 8,90
Categories:
  1. Food
  2. Rent
  3. Transportation
  4. Education
  5. Shopping
  6. Entertainment
  7. Healthcare
  8. Other
  Or type the name of a custom category.
Category: 1
Description (optional): Lunch, Mensa
Date (yyyy-MM-dd, Enter for today 2026-10-27):

Expense recorded: ID 3 | €8.90 | Food | 2026-10-27
Note: You have used 88.2% of your 2026-10 budget. €71.10 left.

...

Choose an option: 7

--- Monthly Financial Summary ---
Month (yyyy-MM, Enter for 2026-10):

Summary for 2026-10 (3 transaction(s))
------------------------------------------------------------
  Income:               €1,450.00
  Expenses:               €528.90
  Savings:                €921.10

Expenses by category:
  Rent                      €520.00  (98.3%)
  Food                        €8.90  (1.7%)

Budget for 2026-10
  Monthly budget:         €600.00
  Spent:                  €528.90
  Remaining:               €71.10
  Used: 88.2%
Note: You have used 88.2% of your 2026-10 budget. €71.10 left.

ID    Date       Type    Category                 Amount  Description
------------------------------------------------------------
1     2026-10-01 Income  Salary                €1,450.00  Werkstudent salary
2     2026-10-02 Expense Rent                    €520.00  October rent
3     2026-10-27 Expense Food                      €8.90  Lunch, Mensa
------------------------------------------------------------
3 transaction(s).
```

Invalid input is explained and asked again instead of crashing the program:

```text
Amount (e.g. 12.50): -3
  Error: Amount must be greater than zero.
Amount (e.g. 12.50): 12.345
  Error: Amount may have at most 2 decimal places.
Date (yyyy-MM-dd, Enter for today 2026-10-27): 2026-13-01
  Error: '2026-13-01' is not a valid date. Use the format yyyy-MM-dd, e.g. 2026-10-27.
```

## Project structure

```text
personal-finance-tracker/
├── pom.xml                         Maven configuration
├── data/                           Runtime data (ignored by Git)
├── docs/ARCHITECTURE.md            Design decisions and class diagram
└── src/
    ├── main/java/com/finance/
    │   ├── Main.java               Entry point: creates and connects all objects
    │   ├── exception/              ValidationException, DataAccessException
    │   ├── model/                  Transaction, TransactionType, Category, Money,
    │   │                           MonthlySummary, BudgetStatus
    │   ├── repository/             TransactionRepository (interface),
    │   │                           CsvTransactionRepository, BudgetRepository,
    │   │                           TransactionCsv, CsvLine, AtomicFileWriter
    │   ├── service/                FinanceService, BudgetService
    │   └── ui/                     ConsoleUI, InputReader, InputParser, MoneyFormatter
    └── test/java/com/finance/      JUnit 5 tests, mirroring the main packages
```

The layers only depend downwards: `ui` → `service` → `repository` → `model`.
See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for details.

## Data storage

Data is stored in the `data/` folder next to where the program is started:

| File | Content |
|---|---|
| `data/transactions.csv` | All transactions (`id,type,date,amount,category,description`) |
| `data/budget.properties` | The monthly budget (`monthly.limit=600.00`) |

- Dates use ISO 8601 (`yyyy-MM-dd`); amounts are stored as plain decimals (`8.90`).
- Descriptions containing commas or quotes are quoted according to the CSV standard.
- Every change is written to a temporary file first, which then replaces the real file.
  A crash while saving therefore never leaves a half-written file.
- On startup every record is validated. If the file has been damaged (for example by
  manual editing), the program reports the exact line and stops **without modifying
  the file**.
- `data/` is listed in `.gitignore`, so personal financial data is never committed.

## Testing

```bash
mvn test
```

The tests cover:

- adding income and expenses, ID assignment and sorting
- totals, balance and monthly summaries (including exact decimal arithmetic)
- filtering by category, date range (inclusive) and month, and description search
- budget calculations and warnings at the 80 %, 100 % and exceeded thresholds
- rejection of invalid amounts, dates, IDs, categories and menu choices
- CSV saving and loading, restart persistence, deletion, quoting, corrupted files
- full user-interface sessions driven by scripted input (no real terminal needed)

Tests that touch files use JUnit's `@TempDir`, so they never read or change real data.

## Known limitations

- Single user and single currency; amounts have exactly two decimal places.
- One overall monthly budget; there are no per-category budgets yet.
- The whole data file is rewritten on every change. This is simple and safe, but would
  be slow with hundreds of thousands of transactions.
- IDs of deleted transactions can be reused if the deleted transaction had the highest ID.
- The CSV export does not protect against spreadsheet formula injection: a description
  starting with `=`, `+`, `-` or `@` may be interpreted as a formula when the file is
  opened in Excel.
- Not designed for running two instances on the same data folder at the same time.

## Future improvements

- Per-category budgets and recurring transactions (e.g. monthly rent)
- Importing bank statements (CSV) with duplicate detection
- Editing existing transactions
- SQLite storage via JDBC as an alternative `TransactionRepository`
- Sanitizing exported cells against formula injection
- Simple text-based charts for monthly spending

## License

This project is licensed under the [MIT License](LICENSE).
