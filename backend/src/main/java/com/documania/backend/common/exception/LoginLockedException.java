package com.documania.backend.common.exception;

public class LoginLockedException extends RuntimeException {

    public LoginLockedException(String message) {
        super(message);
    }
}