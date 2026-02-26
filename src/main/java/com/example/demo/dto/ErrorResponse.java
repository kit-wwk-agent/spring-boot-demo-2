package com.example.demo.dto;

import java.time.LocalDateTime;

public record ErrorResponse(
    String error,
    String message,
    int status,
    LocalDateTime timestamp
) {
    public ErrorResponse(String error, String message, int status) {
        this(error, message, status, LocalDateTime.now());
    }
}
