package com.mora.plan.dto;

public record ErrorResponse(
        String message,
        int status,
        long timestamp
) {}
