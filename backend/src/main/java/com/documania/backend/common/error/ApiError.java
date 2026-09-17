package com.documania.backend.common.error;

import java.time.Instant;
import java.util.Map;

public record ApiError(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    Map<String, String> fieldErrors,
    String traceId
) {

    public ApiError {
        if (fieldErrors == null) {
            fieldErrors = Map.of();
        }
    }

    public ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
    ) {
        this(timestamp, status, error, message, path, fieldErrors, null);
    }
}
