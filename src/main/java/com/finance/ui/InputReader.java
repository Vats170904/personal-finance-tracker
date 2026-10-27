package com.finance.ui;

import com.finance.exception.ValidationException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Function;

/**
 * Reads user input one whole line at a time.
 *
 * <p>Only {@link Scanner#nextLine()} is used. Mixing {@code nextInt()} with
 * {@code nextLine()} leaves the line break in the buffer and causes skipped prompts;
 * reading full lines and parsing them ourselves avoids that problem entirely.
 */
public class InputReader {

    /** Typing this word at any prompt aborts the current action. */
    public static final String CANCEL_KEYWORD = "cancel";

    private final Scanner scanner;
    private final PrintStream out;

    public InputReader(InputStream in, Charset charset, PrintStream out) {
        this.scanner = new Scanner(Objects.requireNonNull(in, "in"), Objects.requireNonNull(charset, "charset"));
        this.out = Objects.requireNonNull(out, "out");
    }

    /**
     * Prints the prompt and returns the trimmed line the user typed.
     *
     * @throws EndOfInputException if the input stream is closed (e.g. Ctrl+D / Ctrl+Z)
     */
    public String readLine(String prompt) {
        out.print(prompt);
        out.flush();
        if (!scanner.hasNextLine()) {
            throw new EndOfInputException();
        }
        return scanner.nextLine().strip();
    }

    /**
     * Keeps asking until {@code parser} accepts the input, printing the error message
     * after each invalid attempt.
     *
     * @throws CancelledException if the user types {@value #CANCEL_KEYWORD}
     */
    public <T> T readValid(String prompt, Function<String, T> parser) {
        while (true) {
            String line = readLine(prompt);
            if (line.equalsIgnoreCase(CANCEL_KEYWORD)) {
                throw new CancelledException();
            }
            try {
                return parser.apply(line);
            } catch (ValidationException e) {
                out.println("  Error: " + e.getMessage());
            }
        }
    }

    /** Asks a yes/no question. "cancel" counts as no. */
    public boolean confirm(String prompt) {
        while (true) {
            String answer = readLine(prompt).toLowerCase(Locale.ROOT);
            switch (answer) {
                case "y", "yes" -> {
                    return true;
                }
                case "n", "no", CANCEL_KEYWORD -> {
                    return false;
                }
                default -> out.println("  Please answer y or n.");
            }
        }
    }

    /** Signals that the user wants to abort the current action and return to the menu. */
    public static final class CancelledException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public CancelledException() {
            super("Cancelled by user");
        }
    }

    /** Signals that there is no more input, so the application should exit. */
    public static final class EndOfInputException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public EndOfInputException() {
            super("End of input");
        }
    }
}
