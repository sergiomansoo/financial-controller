package com.sergio.financial.error;

public class AiUnavailableException extends RuntimeException {
    public AiUnavailableException() {
        super("AI provider unavailable");
    }

    public AiUnavailableException(Throwable cause) {
        super("AI provider unavailable", cause);
    }
}
