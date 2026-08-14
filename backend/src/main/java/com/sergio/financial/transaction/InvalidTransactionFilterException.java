package com.sergio.financial.transaction;

import java.util.Map;

public class InvalidTransactionFilterException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public InvalidTransactionFilterException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
