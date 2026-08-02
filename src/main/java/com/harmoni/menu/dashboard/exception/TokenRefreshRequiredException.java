package com.harmoni.menu.dashboard.exception;

public class TokenRefreshRequiredException extends RuntimeException {

    public TokenRefreshRequiredException(String message) {
        super(message);
    }

    public TokenRefreshRequiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
