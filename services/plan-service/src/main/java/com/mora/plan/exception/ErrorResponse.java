package com.mora.plan.exception;

public record ErrorResponse(
        String message,
        int status,
        long timestamp
) {}
