package com.mora.meeting.dto;

public record ErrorResponse(
        String message,
        int status,
        long timestamp
) {}