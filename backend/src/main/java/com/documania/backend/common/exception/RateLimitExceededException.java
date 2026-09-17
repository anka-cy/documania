package com.documania.backend.common.exception;

/** Limite de tentatives dépassée sur un endpoint public (inscription, réinitialisation…). */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
