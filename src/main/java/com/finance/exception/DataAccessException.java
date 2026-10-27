package com.finance.exception;

/**
 * Thrown when stored data cannot be read or written, or when a stored file is corrupted.
 * Wraps low-level exceptions such as {@link java.io.IOException} so that the service
 * and UI layers do not depend on file-handling details.
 */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
