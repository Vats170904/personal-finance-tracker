package com.finance.exception;

/**
 * Thrown when a value violates a business rule, for example a negative amount
 * or an invalid date. The message is written so it can be shown to the user directly.
 */
public class ValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }
}
