package com.mora.meeting.exception;

public record ErrorResponse(
        String message,
        int status,
        long timestamp
) {}
